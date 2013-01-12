/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package griffon.plugins.gsql

import javax.sql.DataSource

import griffon.core.GriffonApplication
import griffon.plugins.datasource.DataSourceHolder
import griffon.plugins.datasource.DataSourceConnector

/**
 * @author Andres Almiray
 */
@Singleton
final class GsqlConnector {
    private static final String DEFAULT = 'default'
    private bootstrap

    void connect(GriffonApplication app, String dataSourceName = DEFAULT) {
        DataSource dataSource = null
        if(!DataSourceHolder.instance.isDataSourceConnected(dataSourceName)) {
            ConfigObject config = DataSourceConnector.instance.createConfig(app)
            dataSource = DataSourceConnector.instance.connect(app, config, dataSourceName)
        } else {
            dataSource = DataSourceHolder.instance.getDataSource(dataSourceName)
        }

        app.event('GsqlConnectStart', [dataSourceName, dataSource])
        bootstrap = app.class.classLoader.loadClass('BootstrapGsql').newInstance()
        bootstrap.metaClass.app = app
        DataSourceConnector.instance.resolveDataSourceProvider(app).withSql(dataSourceName) { dsName, sql ->
            bootstrap.init(dsName, sql)
        }
        app.event('GsqlConnectEnd', [dataSourceName, dataSource])
    }

    void disconnect(GriffonApplication app, String dataSourceName = DEFAULT) {
        if(!DataSourceHolder.instance.isDataSourceConnected(dataSourceName)) return

        DataSource dataSource = DataSourceHolder.instance.getDataSource(dataSourceName)
        app.event('GsqlDisconnectStart', [dataSourceName, dataSource])
        DataSourceConnector.instance.resolveDataSourceProvider(app).withSql(dataSourceName) { dsName, sql ->
            bootstrap.destroy(dsName, sql)
        }
        app.event('GsqlDisconnectEnd', [dataSourceName, dataSource])
        ConfigObject config = DataSourceConnector.instance.createConfig(app)
        DataSourceConnector.instance.disconnect(app, config, dataSourceName)
    }
}
