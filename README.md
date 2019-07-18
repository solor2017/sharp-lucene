# sharp-lucene
全文检索组件
针对中小型项目快速实现索引增删改查。没必要用zlk那一套，不好维护。  
使用非常简单：
在对应的Field上加上自定义注解即可  
Java客户端使用
使用自定义Annotation注释JavaBean  
自定义的Annotation非常简单，
com.hcq.sharplucene.core.annotation.PKey-- 主键字段标识。  
使用@Pkey注释，表示该字段采用索引、存储、不切分的方式。注意Bean中的主键字段必须和Spring.xml索引配置中的索引主键域名字一致。  
com.hcq.sharplucene.core.annotation.FieldStore--非主键字段的存储标识。  
使用@FieldStore表示这个字段要在索引中存储，不使用表示不存储。  
com.hcq.sharplucene.core.annotation.FieldIndex-- 非主键字段的索引表示。  
不使用@FieldIndex等同于@FieldIndex("NO")表示不索引  
使用@FieldIndex等同于@FieldIndex("NOT_ANALYZED")表示采用索引不分词策略。  
使用@FieldIndex("ANALYZED")表示采用索引且分词策略。  
如果Bean的属性不使用任何 Annotation的标识，则在索引中将忽略这个属性  

JavaBean的Annotation列子：

```java
public class SampleJavaBean implements Serializable {	

	private static final long serialVersionUID = 7153417317917298956L;

	@PKey
	private int commentId;

	@FieldStore( "YES")
	@FieldIndex("NOT_ANALYZED")
	private String userName;

	@FieldStore( "YES")
	private boolean checkFlag;

	@FieldIndex("NOT_ANALYZED")
	private String url;

	@FieldStore( "YES")
	@FieldIndex("NOT_ANALYZED")
	private Date registTime;

	public int getCommentId() {
		return commentId;
	}

	public void setCommentId(int commentId) {
		this.commentId = commentId;
	}

	public String getUserName() {
		return userName;

	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public boolean isCheckFlag() {
		return checkFlag;
	}

	public void setCheckFlag(boolean checkFlag) {
		this.checkFlag = checkFlag;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Date getRegistTime() {
		return registTime;
	}

	public void setRegistTime(Date registTime) {
		this.registTime = registTime;
	}
}
```


本地jar包形式调用如下:

```java
	IndexService indexService = IndexServiceFactory.getLocalIndexService("COMMENT");`
	SampleJavaBean bean = new SampleJavaBean();
	bean.setCheckFlag(true);
	bean.setRegistTime(new Date());
	bean.setUrl("www.baidu.com");
	bean.setUserName("solor");
	bean.setCommentId(20000);
	indexService.add(bean);//这一句就可实现索引创建
```

远程rpc调用：

```java
	IServiceDiscovery serviceDiscovery=new ServiceDiscoveryImpl(Config.getInstance().getKey("zk.address"));
	RpcClientProxy rpcClientProxy=new RpcClientProxy(serviceDiscovery);
	IndexService indexService = rpcClientProxy.clientProxy(IndexService.class, null,"COMMENT")
	indexService.add(bean);//这一句就可实现索引创建
```

查询见sample包里的例子程序  
其它语言远程调用使用http+xml形式
