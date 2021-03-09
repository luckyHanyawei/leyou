package com.leyou.order.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.pojo.Sku;
import com.leyou.order.client.AddressClient;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.dto.AddressDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.enums.PayStatusEnum;
import com.leyou.order.interceptor.LoginInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.mapper.PayLogMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.pojo.PayLog;
import com.leyou.order.utils.PayHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private PayHelper payHelper;

    @Autowired
    private PayLogMapper logMapper;

    @Transactional
    public Long createOrder(OrderDTO orderDTO) {
        // 1、组织订单数据
        Order order = new Order();
        // 1.1.订单id
        long orderId = idWorker.nextId();
        // 1.2.基本信息
        order.setOrderId(orderId);
        order.setCreateTime(new Date());
        order.setPaymentType(orderDTO.getPaymentType());

        // 1.3.组织用户数据
        UserInfo user = LoginInterceptor.getUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getUsername());
        order.setBuyerRate(false);

        // 1.4.收货人信息
        AddressDTO address = AddressClient.findById(orderDTO.getAddressId());
        order.setReceiverMobile(address.getPhone());
        order.setReceiver(address.getName());
        order.setReceiverZip(address.getZipCode());
        order.setReceiverAddress(address.getAddress());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverCity(address.getCity());
        order.setReceiverState(address.getState());

        // 1.5.订单金额
        // 获取id
        Map<Long, Integer> skuNumMap = orderDTO.getCarts().stream()
                .collect(Collectors.toMap(c -> c.getSkuId(), c -> c.getNum()));
        // 查询商品
        List<Sku> skus = goodsClient.querySkuByIds(new ArrayList<>(skuNumMap.keySet()));
        // 定义金额
        long totalPay = 0;

        // 2.新增orderDetail
        List<OrderDetail> details = new ArrayList<>();
        for (Sku sku : skus) {
            Integer num = skuNumMap.get(sku.getId());
            totalPay += sku.getPrice() * num;
            // 组织OrderDetail
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(orderId);
            detail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            detail.setNum(num);
            detail.setSkuId(sku.getId());
            detail.setPrice(sku.getPrice());
            detail.setTitle(sku.getTitle());
            detail.setOwnSpec(sku.getOwnSpec());
            details.add(detail);
        }
        order.setTotalPay(totalPay);
        order.setPostFee(0L);// TODO 应该结合物流计算，在这暂时全场包邮
        order.setActualPay(totalPay + order.getPostFee() ); // TODO 还要减去优惠

        // 新增订单
        orderMapper.insertSelective(order);

        // 新增OrderDetail
        detailMapper.insertList(details);


        // 3.新增orderStatus
        OrderStatus status = new OrderStatus();
        status.setOrderId(orderId);
        status.setCreateTime(new Date());
        status.setStatus(OrderStatusEnum.INIT.value());
        statusMapper.insertSelective(status);

        // 4.减库存
        goodsClient.decreaseStock(orderDTO.getCarts());
        return orderId;
    }

    public Order queryOrderById(Long orderId) {
        // 查询订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if(order == null){
            throw new LyException(HttpStatus.BAD_REQUEST, "订单不存在");
        }
        // 订单详情
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(orderId);
        List<OrderDetail> orderDetails = detailMapper.select(detail);
        order.setOrderDetails(orderDetails);
        // 订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        order.setOrderStatus(orderStatus);
        return order;
    }

    public String getPayUrl(Long orderId) {
        // 查询订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        // 校验订单的状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        if(orderStatus.getStatus() != OrderStatusEnum.INIT.value()){
            throw new LyException(HttpStatus.BAD_REQUEST, "订单状态不正确！");
        }
        // 生成链接， 实付金额应该是order.getActualPay()。我们写为1
        String url = payHelper.createPayUrl(orderId, 1L, "乐优商城测试");

        // 创建支付日志
        // 先删除以前的
        logMapper.deleteByPrimaryKey(orderId);
        // 再重新创建
        PayLog payLog = new PayLog();
        payLog.setOrderId(orderId);
        payLog.setPayType(1);
        payLog.setStatus(PayStatusEnum.NOT_PAY.value());
        payLog.setCreateTime(new Date());
        payLog.setTotalFee(order.getActualPay());
        // 用户信息
        UserInfo user = LoginInterceptor.getUser();
        payLog.setUserId(user.getId());

        logMapper.insertSelective(payLog);
        return url;
    }

    @Transactional
    public void handleNotify(Map<String, String> request) {
        payHelper.handleNotify(request);
    }

    public PayState queryOrderState(Long orderId) {
        // 去数据库查询
        PayLog log = logMapper.selectByPrimaryKey(orderId);

        if(log == null || PayStatusEnum.NOT_PAY.value() == log.getStatus()){
            // log为空，或者未支付，可能是微信的回调失败！我们应该主动去微信查询
            return payHelper.queryPayState(orderId);
        }

        if (PayStatusEnum.SUCCESS.value() == log.getStatus()) {
            return PayState.SUCCESS;
        }
        return PayState.FAIL;
    }
}
