package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Bc4jApplicationModuleConfig")
public interface Bc4jApplicationModuleConfigDescriptor extends Bc4jComponentDescriptor {

    @Property("applicationName")
    String getApplicationName();
    void setApplicationName(String applicationName);

    @Property("deployPlatform")
    String getDeployPlatform();
    void setDeployPlatform(String deployPlatform);

    @Property("jdbcName")
    String getJdbcName();
    void setJdbcName(String jdbcName);

    @Property("poolProperties")
    String[] getPoolProperties();
    void setPoolProperties(String[] poolProperties);

    @Property("databaseProperties")
    String[] getDatabaseProperties();
    void setDatabaseProperties(String[] databaseProperties);

    @Relation("CONFIGURES")
    Bc4jApplicationModuleDescriptor getApplicationModule();
    void setApplicationModule(Bc4jApplicationModuleDescriptor applicationModule);
}
