clone from https://github.com/apereo/cas-overlay-template

# 依赖
跟官方的有所区别，修改了一下
```groovy
if (project.hasProperty("casModules")) {
    def dependencies = project.getProperty("casModules").split(",")
    dependencies.each {
        implementation("org.apereo.cas:$it")
    }
}
```

然后配置`gradle.properties`

```properties
casModules=cas-server-support-jdbc
```

上面是引入了`cas-server-support-jdbc`这个模块，模块可以去[Maven中央仓库](https://search.maven.org/search?q=g:org.apereo.cas)查找一下

# 配置

CAS的配置比较繁杂，但是文档较为齐全，基本上配置都可以找到解释。

## 配置数据库认证

```yaml
cas:
  webflow:
    crypto:
      enabled: false
  locale:
    defaultValue: zh_CN
  authn:
    accept:
      users: ""
    jdbc:
      query:
        - name: customer-user
          order: 1
          url: jdbc:mysql://localhost:3306/cas_sample?characterEcoding=utf-8&useSSL=false&serverTimezone=UTC&rewriteBatchedStatements=true
          user: root
          password: 123456
          driverClass: com.mysql.cj.jdbc.Driver
          dialect: org.hibernate.dialect.PostgreSQL95Dialect
          sql: "SELECT * FROM customer_user WHERE username = ?"
          fieldPassword: password
          fieldDisabled: disabled
          fieldExpired: expired
          principalAttributeList: id,username,password,sex,married,education
          passwordEncoder:
            type: PBKDF2
            secret: "e561a4e6-c82c-11eb-b8bc-0242ac130003"
            strength: 180000
```

使用的数据库脚本

```sql
CREATE DATABASE cas_sample;

CREATE TABLE customer_user
(
    id        int UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username  varchar(20)  NOT NULL COMMENT '用户名',
    password  varchar(255) NOT NULL COMMENT '密码',
    sex       char(1)      NOT NULL COMMENT '性别',
    married   boolean      NOT NULL COMMENT '婚否',
    education tinyint      NOT NULL COMMENT '学历：1大专、2本科、3研究生、4博士、5其他',
    tel       char(11) COMMENT '电话号码',
    email     varchar(200) COMMENT '邮箱',
    address   varchar(200) COMMENT '住址',
    disabled  boolean      NOT NULL DEFAULT FALSE COMMENT '是否禁用',
    expired   boolean      NOT NULL DEFAULT FALSE COMMENT '是否过期'
);

INSERT INTO customer_user (username, password, sex, married, education, tel, email, address)
VALUES ('zhangsan', 'f0147cd7661ca1f3c68b660d3b7fb0ed4c56f45ffaf3fb17fa967b5e1031eedfc9e8b64622b2844b', '男', FALSE, 1,
        '17715182604', 'zhangsan@163.com', '北京市');
INSERT INTO customer_user (username, password, sex, married, education, tel, email, address)
VALUES ('lisi', 'f0147cd7661ca1f3c68b660d3b7fb0ed4c56f45ffaf3fb17fa967b5e1031eedfc9e8b64622b2844b', '男', FALSE, 1,
        '17715182605', 'zhangsan@163.com', '北京市');
INSERT INTO customer_user (username, password, sex, married, education, tel, email, address)
VALUES ('wangwu', 'f0147cd7661ca1f3c68b660d3b7fb0ed4c56f45ffaf3fb17fa967b5e1031eedfc9e8b64622b2844b', '男', FALSE, 1,
        '17715182606', 'zhangsan@163.com', '北京市');
```

到这就配置完毕了，启动下试试（启动比较慢，耐心等等），看到Ready的大字，就证明启动完毕了

![image-20211224144322976](https://cdn.jsdelivr.net/gh/gcdd1993/image-repo@master/img/202112241443283.png)

访问链接http://localhost:8443/cas，输入zhangsan，aaa123456，出现登录成功的提示，并且打印了我们配置的字段`id,username,password,sex,married,education`

![image-20211224144453142](https://cdn.jsdelivr.net/gh/gcdd1993/image-repo@master/img/202112241444223.png)

## 配置Redis存储TGT

上面的例子，我们登录成功后，重启服务，会失去登录信息。因为登录授权信息（可以理解为Session）是存储在内存里的，重启自然就丢失了，所以我们需要将TGT存储到Redis里面。

1、添加模块

```properties
casModules=cas-server-support-redis-ticket-registry
```

2、添加配置

```yaml
cas:
  ticket:
    tgt:
      timeout:
        maxTimeToLiveInSeconds: 172800
    registry:
      redis:
        host: localhost
        port: 16379
```

启动，再次登录测试，登录完毕后，重启服务。

好像不是预想中的维持登陆态，那么我们检查下Redis吧，确实存储了CAS_TICKET

![image-20211224150354788](https://cdn.jsdelivr.net/gh/gcdd1993/image-repo@master/img/202112241503840.png)



## 配置RestApi

[参考文档-Rest](https://apereo.github.io/cas/6.4.x/protocol/REST-Protocol.html)

[参考文档-Swagger](https://apereo.github.io/cas/6.4.x/integration/Swagger-Integration.html)

CAS原生的界面不太好看（丑），一般来说，满足不了业务需求，官方提供了方案，可以通过CSS来修改主题，https://apereo.github.io/cas/6.4.x/ux/User-Interface-Customization.html，不过实际上手有点麻烦，而且无法自定义一些功能。

不过幸运的是，CAS支持RestfulApi，让我们可以自己搞一套前端来实现自定义界面，甚至一些CAS没有的功能，比如接入阿里云无痕验证。

1、添加模块

```groovy
casModules=cas-server-support-rest
```

不需要额外配置，引入依赖即可生效。

启动项目试试看，具体的Api可以在这里看到http://localhost:8443/cas/swagger-ui/index.html

## 登录接口

`POST {{baseUrl}}/v1/tickets`

```
POST {{baseUrl}}/v1/tickets
username:zhangsan
password:aaa123456

<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\"> <html>

<head>
	<title>201 CREATED</title>
</head>

<body>
	<h1>TGT Created</h1>
	<form
		action="http://localhost:8443/cas/v1/tickets/TGT-1-b-IHMDSs5r246FCBs3JApFSDvIC2h2d8mHbeSqEK6TgKvO00Olygr5r-2TcHOjtZmX8-DESKTOP-A340AMR"
		method="POST">Service:<input type="text" name="service" value=""><br><input type="submit" value="Submit"></form>
</body>

</html>
```

登录获取TGT

![image-20211224155056768](https://cdn.jsdelivr.net/gh/gcdd1993/image-repo@master/img/202112241550840.png)

## 服务注册

https://apereo.github.io/cas/6.4.x/services/Service-Management.html

单点登录，最重要的自然就是应用了，一般，一个CAS会负责多个应用

我们使用本地JSON文件的形式来注册服务

1、添加模块

```properties
casModules=cas-server-support-json-service-registry
```

2、添加配置

```yaml
cas:
  serviceRegistry:
    json:
      location: file:/etc/cas/services
```

在本地新建文件夹`D:\etc\cas\services`，并添加文件`demo-10000001.json`，文件名不是乱起的，官方有明确的格式要求

```
JSON fileName = serviceName + "-" + serviceNumericId + ".json"
```

然后重启CAS，我们发现，启动后打印了一行日志

```bash
<Loaded [1] service(s) from [JsonServiceRegistry].>
```

![image-20211224164517546](https://cdn.jsdelivr.net/gh/gcdd1993/image-repo@master/img/202112241645685.png)



