package com.sintef.asam.impl.cam;

public class Header {

	private long protocolVersion;
	private long messageID;
	private long stationID;

	public long getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(long protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public long getMessageID() {
		return messageID;
	}

	public void setMessageID(long messageID) {
		this.messageID = messageID;
	}

	public long getStationID() {
		return stationID;
	}

	public void setStationID(long stationID) {
		this.stationID = stationID;
	}
}
