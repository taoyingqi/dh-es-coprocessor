package com.youzidata.dh.util;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ESClient {
	// ElasticSearch的集群名称
	public static String clusterName;
	// ElasticSearch的host
	public static String nodeHost;
	// ElasticSearch的端口（Java API用的是Transport端口，也就是TCP）
	public static int nodePort;
	// ElasticSearch的索引名称
	public static String indexName;
	// ElasticSearch的类型名称
	public static String typeName;
	// ElasticSearch Client
	public static TransportClient client;

	/**
	 * get Es config
	 *
	 * @return
	 */
	public static String getInfo() {
		List<String> fields = new ArrayList<String>();
		try {
			for (Field f : ESClient.class.getDeclaredFields()) {
				fields.add(f.getName() + "=" + f.get(null));
			}
		} catch (IllegalAccessException ex) {
			ex.printStackTrace();
		}
		return StrUtil.join(fields, ", ");
	}

	/**
	 * init ES client
	 */
	public static void initEsClient() {

		Settings esSettings = Settings.settingsBuilder()
				.put("cluster.name", ESClient.clusterName) //设置ES实例的名称,默认为elasticsearch
//   		    .put("client.transport.sniff", true) //自动嗅探整个集群的状态，把集群中其他ES节点的ip添加到本地的客户端列表中
				.build();
		client = new  TransportClient.Builder().settings(esSettings).build();
		// 添加连接地址
		try {
			client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ESClient.nodeHost),ESClient.nodePort));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			System.out.println("transportClient exception");
			e.printStackTrace();
		}
	}

	/**
	 * Close ES client
	 */
	public static void closeEsClient() {
		client.close();
	}
}
	