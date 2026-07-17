package com.qiujie.dto;

import com.qiujie.enums.BaseEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "数据传输对象", description = "")
public class ResponseDTO {
    @ApiModelProperty("状态码")
    private int code;

    @ApiModelProperty("响应消息")
    private String message;

    @ApiModelProperty("响应数据")
    private Object data;

    @ApiModelProperty("token")
    private String token;

    public ResponseDTO(int code, String message){
        this.code = code;
        this.message = message;
    }

    public ResponseDTO(int code,String message,Object data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public ResponseDTO(BaseEnum e){
        this.code = e.getCode();
        this.message = e.getMessage();
    }

    public ResponseDTO(BaseEnum e, Object data){
        this.code = e.getCode();
        this.message = e.getMessage();
        this.data = data;
    }

    public ResponseDTO(BaseEnum e, Object data, String token){
        this.code = e.getCode();
        this.message = e.getMessage();
        this.data = data;
        this.token = token;
    }

}
