package com.youzidata.dh;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.youzidata.dh.util.ESClient;
import com.youzidata.dh.util.ElasticSearchBulkOperator;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/**
 * 为hbase提供二级索引的协处理器 Coprocesser
 */
public class SyncEsObserver extends BaseRegionObserver {
    private static final Logger LOG = LoggerFactory.getLogger(SyncEsObserver.class);
    //加载配置文件属性
    static Config config = ConfigFactory.load("userdev_pi_solr.properties");

    /**
     * read es config from params
     */
    private static void readConfiguration() {
        ESClient.clusterName = config.getString("es_cluster");
        ESClient.nodeHost = config.getString("es_host");
        ESClient.nodePort = config.getInt("es_port");
        ESClient.indexName = config.getString("es_index");
        ESClient.typeName = config.getString("es_type");
    }

    /**
     *  start
     * @param e
     * @throws IOException
     */
    @Override
    public void start(CoprocessorEnvironment e) throws IOException {
        // read config
        readConfiguration();
        // init ES client
        ESClient.initEsClient();
        LOG.error("------observer init EsClient ------"+ESClient.getInfo());
    }

    /**
     * stop
     * @param e
     * @throws IOException
     */
    @Override
    public void stop(CoprocessorEnvironment e) throws IOException {
        // close es client
        ESClient.closeEsClient();
    }

    /**
     * Called after the client stores a value
     * after data put to hbase then prepare update builder to bulk  ES
     *
     * @param e
     * @param put
     * @param edit
     * @param durability
     * @throws IOException
     */
    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> e, Put put, WALEdit edit, Durability durability) throws IOException {
        String indexId = new String(put.getRow());
        try {
            NavigableMap<byte[], List<Cell>> familyMap = put.getFamilyCellMap();
            Map<String, Object> infoJson = new HashMap<String, Object>();
            Map<String, Object> json = new HashMap<String, Object>();
            for (Map.Entry<byte[], List<Cell>> entry : familyMap.entrySet()) {
                for (Cell cell : entry.getValue()) {
                    String key = Bytes.toString(CellUtil.cloneQualifier(cell));
                    String value = Bytes.toString(CellUtil.cloneValue(cell));
                    json.put(key, value);
                }
            }
            // set hbase family to es
            infoJson.put("info", json);
            ElasticSearchBulkOperator.addUpdateBuilderToBulk(ESClient.client.prepareUpdate(ESClient.indexName, ESClient.typeName, indexId).setDocAsUpsert(true).setDoc(infoJson));
        } catch (Exception ex) {
            LOG.error("observer put  a doc, index [ " + ESClient.indexName + " ]" + "indexId [" + indexId + "] error : " + ex.getMessage());
        }
    }


    /**
     * Called after the client deletes a value.
     * after data delete from hbase then prepare delete builder to bulk  ES
     * @param e
     * @param delete
     * @param edit
     * @param durability
     * @throws IOException
     */
    @Override
    public void postDelete(ObserverContext<RegionCoprocessorEnvironment> e, Delete delete, WALEdit edit, Durability durability) throws IOException {
        String indexId = new String(delete.getRow());
        try {
            ElasticSearchBulkOperator.addDeleteBuilderToBulk(ESClient.client.prepareDelete(ESClient.indexName, ESClient.typeName, indexId));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            LOG.error("observer delete  a doc, index [ " + ESClient.indexName + " ]" + "indexId [" + indexId + "] error : " + ex.getMessage());

        }
    }
}
