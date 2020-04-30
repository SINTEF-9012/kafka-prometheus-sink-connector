package com.sintef.asam.impl;

public interface Message {
	void process(String namespace, PrometheusService service);
}
