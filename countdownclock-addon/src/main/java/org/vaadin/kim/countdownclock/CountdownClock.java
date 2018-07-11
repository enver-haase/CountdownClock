package org.vaadin.kim.countdownclock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.vaadin.kim.countdownclock.client.ui.CountdownClockRpc;
import org.vaadin.kim.countdownclock.client.ui.CountdownClockState;
import org.vaadin.kim.countdownclock.client.ui.CountdownClockState.Direction;

import com.vaadin.ui.AbstractComponent;

public class CountdownClock extends AbstractComponent {

	/**
	 * Interface for listening to countdown events
	 * 
	 * @author Kim
	 * 
	 */
	public interface EndEventListener {
		/**
		 * Listener for countdown events. Takes as input the clock which reached its
		 * target date and time.
		 * 
		 * @param clock
		 */
		public void countDownEnded(CountdownClock clock);
	}

	public static CountdownClock createTimerTo(Date date, String format) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		long difference = calendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

		return createTimer(0, difference, format);
	}

	public static CountdownClock createCountdownTo(Date date, String format) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		long difference = calendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

		return createCountdown(difference, 0L, format);
	}

	public static CountdownClock createCountdown(long fromMillis, String format) {
		return createCountdown(fromMillis, 0L, format);
	}

	public static CountdownClock createCountdown(long fromMillis, Long toMillis, String format) {
		CountdownClock clock = new CountdownClock();
		clock.setTime(fromMillis);
		clock.setTargetTime(toMillis);
		clock.setDirection(Direction.DOWN);
		clock.setFormat(format);
		return clock;
	}

	public static CountdownClock createTimer(String format) {
		return createTimer(0, format);
	}

	public static CountdownClock createTimer(long fromMillis, String format) {
		return createTimer(fromMillis, null, format);
	}

	public static CountdownClock createTimer(long fromMillis, Long toMillis, String format) {
		CountdownClock clock = new CountdownClock();
		clock.setTime(fromMillis);
		clock.setTargetTime(toMillis);
		clock.setDirection(Direction.UP);
		clock.setFormat(format);
		return clock;
	}

	private static final long serialVersionUID = -4093579148150450057L;

	protected List<EndEventListener> listeners = new ArrayList<EndEventListener>();

	private boolean autoStart = true;

	public CountdownClock() {
		CountdownClockRpc rpc = new CountdownClockRpc() {
			private static final long serialVersionUID = -7392569455421206075L;

			public void countdownEnded() {
				for (EndEventListener listener : listeners) {
					listener.countDownEnded(CountdownClock.this);
				}
			}
		};
		registerRpc(rpc);
	}

	@Override
	protected CountdownClockState getState() {
		return (CountdownClockState) super.getState();
	}

	private long getInitialTime() {
		return getState().getCounterStart();
	}

	public void setTime(long millis) {
		stop();
		getState().setCounterStart(millis);
	}

	public Long getTargetTime() {
		return getState().getCounterTarget();
	}

	public void setTargetTime(Long millis) {
		getState().setCounterTarget(millis);
	}

	public Direction getDirection() {
		return getState().getCounterDirection();
	}

	public void setDirection(Direction direction) {
		getState().setCounterDirection(direction);
	}

	public void start() {
		getState().setActive(true);
	}
	
	public void start(long startMillis, long targetMillis) {
		setTime(startMillis);
		setTargetTime(targetMillis);
		start();
	}
	
	public void start(long startMillis, Direction direction) {
		setTime(startMillis);
		setDirection(direction);
		start();
	}

	public void stop() {
		getState().setActive(false);
	}

	/**
	 * Get the current format being used
	 * 
	 * @return
	 */
	public String getFormat() {
		return getState().getTimeFormat();
	}

	/**
	 * Set the format for the clock. Available parameters:</br>
	 * </br>
	 * 
	 * %- minus sign if the time is negative or empty string</br>
	 * %d days </br>
	 * %h hours </br>
	 * %m minutes </br>
	 * %s seconds </br>
	 * %ts tenth of a seconds </br>
	 * 
	 * For example "%d day(s) %h hour(s) and %m minutes" could produce the string "2
	 * day(s) 23 hour(s) and 5 minutes"
	 * 
	 * @param format
	 */
	public void setFormat(String format) {
		getState().setTimeFormat(format);
	}

	public boolean getNeglectHigherUnits() {
		return getState().isNeglectHigherUnits();
	}

	/**
	 * Neglegting higher units means that e.g. the number seconds is always shown as
	 * 0-59, even if there are a number of minutes left that are not shown (no %m in
	 * the format).
	 *
	 * @param neglect
	 */
	public void setNeglectHigherUnits(boolean neglect) {
		getState().setNeglectHigherUnits(neglect);
	}

	/**
	 * Neglegting higher units means that e.g. the number seconds is always shown as
	 * 0-59, even if there are a number of minutes left that are not shown (no %m in
	 * the format).
	 *
	 * @param neglect
	 */
	public CountdownClock withNeglectHigherUnits(boolean neglect) {
		setNeglectHigherUnits(neglect);
		return this;
	}

	public boolean isContinueAfterEnd() {
		return getState().isContinueAfterEnd();
	}

	public void setContinueAfterEnd(boolean continueAfterEnd) {
		getState().setContinueAfterEnd(continueAfterEnd);
	}

	public CountdownClock continueAfterEnd(boolean continueAfterEnd) {
		setContinueAfterEnd(continueAfterEnd);
		return this;
	}

	public boolean isAutoStart() {
		return autoStart;
	}

	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	@Override
	public void beforeClientResponse(boolean initial) {
		if(getFormat() == null) {
			throw new IllegalStateException("Should set a format");
		}
		if (getDirection() == null) {
			if (getTargetTime() == null) {
				throw new IllegalStateException("Eiter TargetTime or Direction must be set");
			}
			if (getInitialTime() < getTargetTime()) {
				setDirection(Direction.UP);
			} else {
				setDirection(Direction.DOWN);
			}
		} else if (getDirection() == Direction.UP && getInitialTime() > getTargetTime()) {
			throw new IllegalArgumentException("If direction is UP, counterStart should be lesl than counterTarget");
		} else if (getDirection() == Direction.DOWN && getInitialTime() < getTargetTime()) {
			throw new IllegalArgumentException(
					"If direction is DOWN, counterStart should be greather than counterTarget");
		}

		if (autoStart == true) {
			start();
		}
		super.beforeClientResponse(initial);
	}

	/**
	 * Add a listener for countdown events.
	 * 
	 * @param listener
	 */
	@Deprecated
	public void addListener(EndEventListener listener) {
		addEndEventListener(listener);
	}

	/**
	 * Add a listener for countdown events.
	 * 
	 * @param listener
	 */
	public void addEndEventListener(EndEventListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	/**
	 * Remove listener for countdown events.
	 * 
	 * @param listener
	 */
	@Deprecated
	public void removeListener(EndEventListener listener) {
		removeEndEventListener(listener);
	}

	/**
	 * Remove listener for countdown events.
	 * 
	 * @param listener
	 */
	public void removeEndEventListener(EndEventListener listener) {
		if (listener != null) {
			listeners.remove(listener);
		}
	}
}
