package jxnu.hc_re.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface FileService {
    /** 将请求中的 filepath 解析为服务器上的真实 File 对象 */
    File resolveFile(String filepath);

    /** 获取文件的 MIME 类型 */
    String probeContentType(File file) throws IOException;

    /** 打开文件输入流 */
    InputStream openStream(File file) throws IOException;
}
