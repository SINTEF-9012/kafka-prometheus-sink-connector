package com.sintef.asam.impl.cam;

import com.sintef.asam.impl.Message;
import com.sintef.asam.impl.PrometheusService;
import com.sintef.asam.impl.TimeoutGauge;

public class CAM implements Message {// TODO: currently just a mock-up of CAM messages...

	private Header header;
	private CAMField cam;

	public Header getHeader() {
		return header;
	}

	public void setHeader(Header header) {
		this.header = header;
	}

	public CAMField getCam() {
		return cam;
	}

	public void setCam(CAMField cam) {
		this.cam = cam;
	}

	@Override
	public void process(String namespace, PrometheusService service) {
		final long stationID = header.getStationID();
		final float speed = cam.getSpeedValue();
		final float heading = cam.getHeadingValue();

		final TimeoutGauge speedGauge = service.getFactory().createOrGetGauge(namespace, Long.toString(stationID), "speedValue");
		speedGauge.update(speed);

		final TimeoutGauge headingGauge = service.getFactory().createOrGetGauge(namespace, Long.toString(stationID), "headingValue");
		headingGauge.update(heading);
	}

}
