package com.ypw.starter.shardingsphere.config;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.google.common.collect.Lists;
import io.shardingsphere.api.config.rule.ShardingRuleConfiguration;
import io.shardingsphere.api.config.rule.TableRuleConfiguration;
import io.shardingsphere.api.config.strategy.InlineShardingStrategyConfiguration;
import io.shardingsphere.api.config.strategy.StandardShardingStrategyConfiguration;
import io.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties({FirstDsProp.class, SecondDsProp.class})
@EnableTransactionManagement(proxyTargetClass = true)
@Slf4j
public class DataSourceConfig {

    /**
     * druid数据源1
     *
     * @param firstDSProp
     * @return
     */
    @Bean("ds0")
    public DataSource ds0(FirstDsProp firstDSProp) {
        Map<String, Object> dsMap = new HashMap<>();
        dsMap.put("type", firstDSProp.getType());
        dsMap.put("url", firstDSProp.getJdbcUrl());
        dsMap.put("username", firstDSProp.getUsername());
        dsMap.put("password", firstDSProp.getPassword());

        DruidDataSource ds = (DruidDataSource) buildDataSource(dsMap);
        ds.setProxyFilters(Lists.newArrayList(statFilter()));
        // 每个分区最大的连接数
        ds.setMaxActive(20);
        // 每个分区最小的连接数
        ds.setMinIdle(5);

        return ds;
    }

    /**
     * druid数据源2
     *
     * @param secondDsProp
     * @return
     */
    @Bean("ds1")
    public DataSource ds1(SecondDsProp secondDsProp) throws SQLException {
        Map<String, Object> dsMap = new HashMap<>();
        dsMap.put("type", secondDsProp.getType());
        dsMap.put("url", secondDsProp.getJdbcUrl());
        dsMap.put("username", secondDsProp.getUsername());
        dsMap.put("password", secondDsProp.getPassword());

        DruidDataSource ds = (DruidDataSource) buildDataSource(dsMap);
        //ds.setProxyFilters(Lists.newArrayList(statFilter()));
        // 每个分区最大的连接数
        ds.setMaxActive(20);
        // 每个分区最小的连接数
        ds.setMinIdle(5);
        ds.setFilters("stat");
        ds.setMaxActive(20);
        ds.setInitialSize(1);
        ds.setMaxWait(60000);
        ds.setMinIdle(10);
        ds.setTimeBetweenEvictionRunsMillis(60000);
        ds.setMinEvictableIdleTimeMillis(300000);
        ds.setValidationQuery("SELECT 1 FROM DUAL");
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(true);
        ds.setPoolPreparedStatements(true);
        ds.setMaxOpenPreparedStatements(2001 - 2018);
        return ds;
    }

    @Bean
    public Filter statFilter() {
        StatFilter filter = new StatFilter();
        filter.setSlowSqlMillis(5000);
        filter.setLogSlowSql(true);
        filter.setMergeSql(true);
        return filter;
    }

    @Bean
    public ServletRegistrationBean statViewServlet() {
        //创建servlet注册实体
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new StatViewServlet(), "/druid/*");
        //设置ip白名单
        servletRegistrationBean.addInitParameter("allow", "127.0.0.1");
        //设置控制台管理用户
        servletRegistrationBean.addInitParameter("loginUsername", "admin");
        servletRegistrationBean.addInitParameter("loginPassword", "123456");
        //是否可以重置数据
        servletRegistrationBean.addInitParameter("resetEnable", "false");
        return servletRegistrationBean;
    }

    /**
     * shardingjdbc数据源
     *
     * @return
     * @throws SQLException
     */
    @Bean("dataSource")
    public DataSource dataSource(@Qualifier("ds0") DataSource ds0, @Qualifier("ds1") DataSource ds1) throws SQLException {
        // 配置真实数据源
        Map<String, DataSource> dataSourceMap = new HashMap<>();
        dataSourceMap.put("ds0", ds0);
        dataSourceMap.put("ds1", ds1);
        // 配置分片规则
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        //shardingRuleConfig.getTableRuleConfigs().add(userRuleConfig());
        //shardingRuleConfig.getTableRuleConfigs().add(addressRuleConfig());
        shardingRuleConfig.getTableRuleConfigs().add(orderRuleConfig());
        shardingRuleConfig.getTableRuleConfigs().add(orderItemRuleConfig());
        //默认分库规则
        shardingRuleConfig.setDefaultDatabaseShardingStrategyConfig(new InlineShardingStrategyConfiguration("user_id", "ds$->{user_id % 2}"));
        //绑定表规则列表
        shardingRuleConfig.getBindingTableGroups().add("t_order, t_order_item");
        //广播表
        shardingRuleConfig.getBroadcastTables().add("t_product");

        Properties p = new Properties();
        p.setProperty("sql.show", Boolean.TRUE.toString());
        // 获取数据源对象
        DataSource dataSource = ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingRuleConfig, new HashMap<String, Object>(), p);
        return dataSource;
    }

    /**
     * 需要手动配置事务管理器
     *
     * @param dataSource
     * @return
     */
    @Bean
    public DataSourceTransactionManager transactitonManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean("sqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        //bean.setConfigLocation(new ClassPathResource("mybatis-config.xml"));
        return bean.getObject();
    }

    @Bean("sqlSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }


    private TableRuleConfiguration orderRuleConfig() {
        TableRuleConfiguration tableRuleConfig = new TableRuleConfiguration();
        tableRuleConfig.setLogicTable("t_order");
        tableRuleConfig.setActualDataNodes("ds${0..1}.t_order_${0..1}");
        tableRuleConfig.setKeyGeneratorColumnName("order_id");
        tableRuleConfig.setKeyGenerator(new SnowflakeShardingKeyGenerator(0L, 1L));
        //分库策略，缺省表示使用默认分库策略
        //tableRuleConfig.setDatabaseShardingStrategyConfig(new StandardShardingStrategyConfiguration("user_id", new IdShardingAlgorithm(), new IdShardingAlgorithm()));
        //分表策略，缺省表示使用默认分表策略
        tableRuleConfig.setTableShardingStrategyConfig(new InlineShardingStrategyConfiguration("order_id", "t_order$->{order_id % 2}"));
        return tableRuleConfig;
    }

    private TableRuleConfiguration orderItemRuleConfig() {
        TableRuleConfiguration tableRuleConfig = new TableRuleConfiguration();
        tableRuleConfig.setLogicTable("t_order_item");
        tableRuleConfig.setActualDataNodes("ds${0..1}.t_order_item_${0..1}");
        tableRuleConfig.setKeyGeneratorColumnName("order_item_id");
        tableRuleConfig.setKeyGenerator(new SnowflakeShardingKeyGenerator(0L, 2L));
        //分库策略，缺省表示使用默认分库策略
        //tableRuleConfig.setTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("id", idShardingAlgorithm));
        //分表策略，缺省表示使用默认分表策略
        tableRuleConfig.setTableShardingStrategyConfig(new InlineShardingStrategyConfiguration("order_item_id", "t_order_item$->{order_item_id % 2}"));
        return tableRuleConfig;
    }


    public static DataSource buildDataSource(Map<String, Object> dataSourceMap) {
        Object type = dataSourceMap.get("type");
        try {
            Class<? extends DataSource> dataSourceType;
            dataSourceType = (Class<? extends DataSource>) Class.forName((String) type);
            //String driverClassName = dataSourceMap.get("driver").toString();
            String url = dataSourceMap.get("url").toString();
            String username = dataSourceMap.get("username").toString();
            String password = dataSourceMap.get("password").toString();
            // 自定义DataSource配置
            DataSourceBuilder factory = DataSourceBuilder.create().url(url).username(username).password(password).type(dataSourceType);
            return factory.build();
        } catch (Exception e) {
            log.error("构建数据源" + type + "出错", e);
        }
        return null;
    }

}