package eu.dkvz;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatasourcesConfiguration {

  @Bean(name = "dsPrimary")
  @Primary
  @ConfigurationProperties(prefix="spring.datasource")
  public DataSource primaryDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "dsStats")
  @ConfigurationProperties(prefix="spring.stats-datasource")
  public DataSource statsDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "jdbcPrimary")
  @Primary
  @Autowired
  public JdbcTemplate jdbcTpl(@Qualifier("dsPrimary") DataSource dsPrimary) {
    return new JdbcTemplate(dsPrimary);
  }

  @Bean(name = "jdbcStats")
  @Autowired
  public JdbcTemplate jdbcTplStats(@Qualifier("dsStats") DataSource dsStats) {
    return new JdbcTemplate(dsStats);
  }

}