package com.hcq.sharplucene.rpc;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:类似spring的beanDefintion作用
 */
@Data
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = -9100893052391757993L;
    private String className;
    private String methodName;
    private Object[] parameters;

    private String version;
    private Object[] constryctArgs;
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
}
