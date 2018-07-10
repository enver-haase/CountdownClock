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
	 * Number of milliseconds the counter should start from
	 */
	private long counterStart;

	/**
	 * Number of milliseconds to count to
	 */
	private Long counterTarget;

	private Direction counterDirection;

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

	public long getCounterStart() {
		return counterStart;
	}
	
	public void setCounterStart(long counterStart) {
		this.counterStart = counterStart;
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

	public void setCounter(long counterInitalMillis, Long counterTargetMillis, Direction countDirection) {
		this.setCounterStart(counterInitalMillis);
		this.setCounterTarget(counterTargetMillis);
		this.setCounterDirection(countDirection);
	}

	public void setCounter(long startMillis, long endMillis, Direction direction, String format) {
		setCounter(startMillis, endMillis, direction);
		this.setTimeFormat(format);
	}

}
