package JavaDemo;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.auth.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

public class JwtTest {

    private static final String pubKeyPath = "D:\\heima\\rsa\\rsa.pub";

    private static final String priKeyPath = "D:\\heima\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "2345");
    }

    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        // 生成token
        String token = JwtUtils.generateTokenInSeconds(new UserInfo(20L, "jack"), privateKey, 60);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MjAsInVzZXJuYW1lIjoiamFjayIsImV4cCI6MTYxNDU4MTIwMH0.FrmN_Xr9BXs9ic1FRCvZZ17-7BtPB0iglLhp0j9oPhlhRVildp7gt_D9LeQUBFOHh3rl0flzKtd8s3ANINpR9TIr4BLyq7EnwOfPbq4wGWx_Vdp3x0D5du32D_TKtM-nLeXRua_CcWFR7JdBAFryOvKsv_w3BxL0wCKgSF_9H_A";
        // 解析token
        UserInfo user = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + user.getId());
        System.out.println("userName: " + user.getUsername());
    }
}