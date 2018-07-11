package org.vaadin.kim.countdownclock.client.ui;

import com.vaadin.shared.communication.ClientRpc;

public interface CountdownClockClientRpc extends ClientRpc {

	void start();
	
	void stop();

	void setTime(long millis);
}
