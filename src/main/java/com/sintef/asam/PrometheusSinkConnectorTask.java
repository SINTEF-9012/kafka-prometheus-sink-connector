package com.sintef.asam;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONException;
import com.sintef.asam.impl.PrometheusService;

public class PrometheusSinkConnectorTask extends SinkTask {

	private static final Logger logger = LogManager.getLogger(PrometheusSinkConnectorTask.class);

	private PrometheusService service;
	private Class<?> deserializer;

	@Override
	public String version() {
		return VersionUtil.getVersion();
	}

	@Override
	public void start(Map<String, String> map) {
		final PrometheusSinkConnectorConfig cfg = new PrometheusSinkConnectorConfig(map);
		try {			
			deserializer = Class.forName(cfg.getDeserializer());
			service = new PrometheusService(cfg);
		} catch (IOException e) {
			final String err = "Could not start Prometheus service: " + e.getMessage();
			System.err.println(err);
			logger.error(err);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			final String err = "Could not load deserializer class " + cfg.getDeserializer() + ": " + e.getMessage();
			System.err.println(err);
			logger.error(err);
			e.printStackTrace();
		}
	}

	@Override
	public void put(Collection<SinkRecord> collection) {
		for (Iterator<SinkRecord> sinkRecordIterator = collection.iterator(); sinkRecordIterator.hasNext();) {
			final SinkRecord sinkRecord = sinkRecordIterator.next();
			final String namespace = sinkRecord.topic();
			final Object key = sinkRecord.key();
			final int partition = sinkRecord.kafkaPartition();
			logger.debug("Received record: '{}' on topic.partition: '{}'.'{}' witk key: '{}'", sinkRecord.value(), namespace, partition, key);
			System.out.println("Received record: " + sinkRecord.value() + " on topic.partition: " + namespace + "." + partition + " witk key: " + key);
			try {
				final String stringSinkRecord = new String((byte[]) sinkRecord.value(), "UTF-8");
				service.process(namespace, stringSinkRecord, deserializer);
			} catch (JSONException e) {
				logger.error("Could not convert record to JSON '{}'", sinkRecord);
			} catch (UnsupportedEncodingException e) {
				logger.error("Could not convert record to JSON '{}': '{}'", sinkRecord, e.getMessage());
			}
		}
	}

	@Override
	public void stop() {
		logger.warn("STOPPING prometheus task");
		System.err.println("STOPPING prometheus task");
		service.stop();
	}
}
