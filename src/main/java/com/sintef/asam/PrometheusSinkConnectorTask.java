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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.sintef.asam.impl.PrometheusService;


public class PrometheusSinkConnectorTask extends SinkTask {

	private static final Logger logger = LogManager.getLogger(PrometheusSinkConnectorTask.class);
	
	private PrometheusService service;
	
	@Override
    public String version() {
        return VersionUtil.getVersion();
    }


    @Override
    public void start(Map<String, String> map) {
    	try {
			service = new PrometheusService(new PrometheusSinkConnectorConfig(map));
		} catch (IOException e) {
			final String err = "Could not start Prometheus service: " + e.getMessage();
			System.err.println(err);
			logger.error(err);
			e.printStackTrace();
		}
    }

	@Override
	public void put(Collection<SinkRecord> collection) {
		for (Iterator<SinkRecord> sinkRecordIterator = collection.iterator(); sinkRecordIterator.hasNext(); ) {
			SinkRecord sinkRecord = sinkRecordIterator.next();
			logger.debug("Received record: '{}'", sinkRecord.value());
			JSONObject jsonSinkRecord;
			try {
				String stringSinkRecord = new String((byte[])sinkRecord.value(), "UTF-8");
				jsonSinkRecord = JSON.parseObject(stringSinkRecord);
				String subsystem = jsonSinkRecord.getString("ss");
				String namespace = jsonSinkRecord.getString("ns");;
				long temperature = jsonSinkRecord.getLong("t");
				long humidity = jsonSinkRecord.getLong("h");
				service.process(subsystem, namespace, "t", temperature);
				service.process(subsystem, namespace, "h", humidity);
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
