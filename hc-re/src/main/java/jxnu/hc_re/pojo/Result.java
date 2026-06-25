package jxnu.hc_re.pojo;

import lombok.Data;

/**
 * 后端统一返回结果
 */
@Data
public class Result<T> {  // 加上泛型
    private Integer code;
    private String msg;
    private T data;       // 泛型类型
    
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "success";
        return result;
    }
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "success";
        result.data = data;
        return result;
    }
    
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.code = 0;
        result.msg = msg;
        return result;
    }
}