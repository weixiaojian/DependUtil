# 常用依赖及工具类
> 基于SpringBoot平台 
```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.3.3.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>io.imwj</groupId>
    <artifactId>DependUtil</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <!-- SpringBoot依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!--hutool工具类-->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.4.3</version>
        </dependency>

        <!--lombok-->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- fastjson -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.6</version>
        </dependency>

        <!--Oracle / DataSource -->
        <dependency>
            <groupId>com.oracle</groupId>
            <artifactId>ojdbc6</artifactId>
            <version>12.1.0.1-atlassian-hosted</version>
        </dependency>

        <!-- Druid连接池 -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-starter</artifactId>
            <version>1.1.10</version>
        </dependency>

        <!--mybatis-plus-->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.4.0</version>
        </dependency>

        <!--mybatis plus代码生成-->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-generator</artifactId>
            <version>3.3.2</version>
        </dependency>
        <dependency>
            <groupId>org.freemarker</groupId>
            <artifactId>freemarker</artifactId>
            <version>2.3.29</version>
        </dependency>

        <!--aspectj aop切面-->
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
        </dependency>

        <!--接口api文档生成-->
        <dependency>
            <groupId>io.github.yedaxia</groupId>
            <artifactId>japidocs</artifactId>
            <version>1.4.1</version>
        </dependency>

        <!--读取配置文件数据WxConfig用-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <!--加载配置文件-->
        <resources>
            <resource>
                <directory>src/main/java</directory>
                <includes>
                    <include>**/**</include>
                </includes>
                <filtering>false</filtering>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <includes>
                    <include>**/**</include>
                </includes>
                <filtering>false</filtering>
            </resource>
        </resources>

        <plugins>
            <!--使用JDK1.8-->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
        <finalName>DependUtil</finalName>
    </build>
</project>
```

# oracle中的一些相关应用
## 序列
* Oracle表中的主键是无法自增的，如果要实现主键自增且主键不重复那就要使用序列
* 创建序列：SEQ_CREATE_ID
```
create sequence SEQ_CREATE_ID   --序列名称
minvalue 1                      --最小值
maxvalue 999999999999999        --最大值
start with 1                    --起始值
increment by 1                  --增量
cache 20;                       --高速缓存大小
```
* 使用序列
```
--触发器中直接使用
 insert into tab_modify(id,bill_code,modifier,TYPE)
 values(SEQ_CREATE_ID.nextval,:new.order_bill,v_modifier,'insert');
 
--先查询出来再使用
SELECT SEQ_CREATE_ID.nextval AS ID from dual
```

## 触发器
* 取旧值`:old.order_bill`
* 取新值`:new.order_bill`
* 触发器里变量赋值
```
  SELECT modifier
    INTO v_modifier
  FROM DUAL
```
* 问题场景：业务中订单表数据量过大（过亿），用户需要查询最近三个月的数据，根据指定字段模糊查询等等
* 1.建表时：数据库层面针对订单的创建时间进行时间分区，七天一分区/一个月一分区等等
* 2.订单表的数据来源有七八个平台，针对不同平台的数据进行分表处理：将主键、涉及查询条件的字段、日期等插入到子表中（此处用触发器实现 也可用Java插入）
```
CREATE OR REPLACE TRIGGER TRG_CREATE_ORDER_XCX
  AFTER INSERT OR DELETE OR UPDATE OF ORDER_BILL, BILL_CODE ON TAB_ORDER
  FOR EACH ROW

DECLARE
  /**
  *   如果是微信/小程序下单，将单号记录到微信订单专用表 tab_order_wechat
  *   since 20210511 by wj
  *   create 2021-05-11 16:57:09
  */
BEGIN
  IF INSERTING THEN
    IF NVL(:NEW.OPEN_ID, '*') <> '*' AND NVL(:NEW.DATA_FROM, '*') = '微信' THEN
      INSERT INTO TAB_ORDER_WECHAT
        (ORDER_BILL, OPEN_ID, CREATE_DATE, BILL_CODE)
      VALUES
        (:NEW.ORDER_BILL, :NEW.OPEN_ID, :NEW.CREATE_DATE, :NEW.BILL_CODE);
    END IF;
  END IF;
  IF UPDATING THEN
    IF NVL(:NEW.OPEN_ID, '*') <> '*' AND NVL(:NEW.DATA_FROM, '*') = '微信' THEN
      UPDATE TAB_ORDER_WECHAT
         SET BILL_CODE = :NEW.BILL_CODE, 
				     OPEN_ID = :NEW.OPEN_ID
       WHERE ORDER_BILL = :NEW.ORDER_BILL;
    END IF;
  END IF;
  IF DELETEING THEN
    IF NVL(:NEW.OPEN_ID, '*') <> '*' AND NVL(:NEW.DATA_FROM, '*') = '微信' THEN
    --此处去删除订单主表中的数据
    ...
    END IF;
  END IF;
END TRG_CREATE_ORDER_XCX;
```
* 3.查询时优先查子表 然后以子表的主键去关联主表的数据，也可子表left join主表
```
SELECT /*+index(T,TAB_ORDER_P)*/
 T.*
  FROM TAB_ORDER T
 WHERE T.ORDER_BILL IN (SELECT /*+index(w,TAB_ORDER_WECHAT_I)*/
                         W.ORDER_BILL
                          FROM TAB_ORDER_WECHAT W
                         WHERE W.CREATE_DATE > SYSDATE - 180
                           AND W.OPEN_ID = ?)
 ORDER BY CREATE_DATE DESC
```

* 创建触发器自动记录表的数据变更日志（新增/修改/删除）
```
create or replace trigger trg_modify_order
after insert or delete or update of  order_bill(此处可以针对指定字段变更触发，以逗号隔开) for each row
declare
   /**
   *   自动产生修改记录触发器脚本 version 1.0.0
   *   since 20210829 by wj
   *   create 2021-08-29 11:08:09
   */
   v_modifier               varchar2(30); --修改人
   v_modifier_code          varchar2(20); --修改人编码
begin
    --修改人变量赋值（假设dual表里有单条数据）
    SELECT modifier
     INTO v_modifier
    FROM DUAL
      
   if inserting then
     --插入到指定日志表（类型为insert，插入状态下只有新值没有旧值）
     insert into tab_modify(id,bill_code,modifier,TYPE)
     values(seq_modify.nextval,:new.order_bill,v_modifier,'insert');
   end if;

   if updating then
     --插入到指定日志表（类型为update，此处是判断bill_code字段值变更了）
     if nvl(:old.bill_code,'!@#')<>nvl(:new.bill_code,'!@#') then
       insert into tab_modify(id,旧值,新值,TYPE)
       values(seq_modify.nextval,:old.bill_code,:new.bill_code,'update');
     end if;
   end if;

   if deleting then
     --插入到指定日志表（类型为delete，删除状态下只有旧值没有新值）
     insert into tab_modify(id,旧值,新值,TYPE)
     values(seq_modify.nextval,:old.bill_code,'','update');
   end if;
end trg_modify_order;
```

## 刷表数据
* 场景：个别情况下 表中产生了一部分错误数据，我们需要将这部分数据修复
* 解决方案：我们先将表的所有数据查询出来(数据量大的话要分时间段)，然后筛选出符合条件的数据 进行更新修复
```
DECLARE
  V_COUNT      NUMBER;
BEGIN
  FOR PERDATA IN (SELECT * FROM TAB_WX_ADDRESS X WHERE X.BL_TYPE IS NULL) LOOP
    SELECT COUNT(1)
      INTO V_COUNT
      FROM TAB_ORDER T
     WHERE NVL(T.DATA_FROM, '*') = '微信'
       AND T.ACCEPT_MAN_MOBILE = PERDATA.REMARK;
    IF V_COUNT > 0 THEN
      -- dbms_output.put_line('guid='|| perData.guid||',  phone='||perData.user_phone||', 匹配='||v_count);
      UPDATE TAB_WX_ADDRESS T
         SET T.BL_TYPE = 1
       WHERE T.GUID = PERDATA.GUID;
      COMMIT;
    END IF;
  END LOOP;
END;
```