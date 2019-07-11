package com.hcq.sharplucene.util;

import java.util.ResourceBundle;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public enum Config {

    SINGLETON_INSTANCE(ResourceBundle.getBundle("lucene-server"));

    private ResourceBundle bundle;
    public static final String LUCENE_PROVIDER="lucene.provider";


    public String getProvider(){
        return this.getKey(Config.LUCENE_PROVIDER);
    }
    public String getProvider(String key){
        return this.getKey(key);
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public String getKey(String key){
        return bundle.getString(key);
    }




    Config(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public static Config getInstance() {
        return SINGLETON_INSTANCE;
    }

}
