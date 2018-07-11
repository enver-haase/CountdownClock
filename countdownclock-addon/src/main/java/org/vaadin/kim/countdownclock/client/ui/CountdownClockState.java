package org.vaadin.kim.countdownclock.client.ui;

import com.vaadin.shared.AbstractComponentState;

public class CountdownClockState extends AbstractComponentState {

	public static enum Direction {
		UP, DOWN
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -2111850091485279585L;

	/**
	 * Defines the format in which the countdown clock should show the remaining
	 * time
	 */
	private String timeFormat;

	/**
	 * Number of milliseconds to count to
	 */
	private Long counterTarget;

	private Direction counterDirection;

	private boolean autostart = false;

	private boolean continueAfterEnd = false;

	/**
	 *
	 */
	private boolean neglectHigherUnits;

	public void setNeglectHigherUnits(boolean neglect) {
		this.neglectHigherUnits = neglect;
	}

	public boolean isNeglectHigherUnits() {
		return neglectHigherUnits;
	}

	public String getTimeFormat() {
		return timeFormat;
	}

	public void setTimeFormat(String timeFormat) {
		this.timeFormat = timeFormat;
	}

	public Long getCounterTarget() {
		return counterTarget;
	}

	public void setCounterTarget(Long counterTarget) {
		this.counterTarget = counterTarget;
	}

	public Direction getCounterDirection() {
		return counterDirection;
	}

	public void setCounterDirection(Direction counterDirection) {
		this.counterDirection = counterDirection;
	}

	public boolean isContinueAfterEnd() {
		return continueAfterEnd;
	}

	public void setContinueAfterEnd(boolean continueAfterEnd) {
		this.continueAfterEnd = continueAfterEnd;
	}

	public boolean isAutostart() {
		return autostart;
	}

	public void setAutostart(boolean active) {
		this.autostart = active;
	}

}
