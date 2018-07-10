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
		 * Listener for countdown events. Takes as input the clock which reached
		 * its target date and time.
		 * 
		 * @param clock
		 */
		public void countDownEnded(CountdownClock clock);
	}
	
	private static final long serialVersionUID = -4093579148150450057L;

	protected String format = "%dD %hH %mM %sS";

	protected List<EndEventListener> listeners = new ArrayList<EndEventListener>();

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
	public CountdownClockState getState() {
		return (CountdownClockState) super.getState();
	}

	public CountdownClock startTimerTo(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		long difference = calendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

		startTimer(0, difference);
		return this;
	}
	
	public CountdownClock startCountdownTo(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		long difference = calendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

		startCountdown(difference, 0L);
		return this;
	}

	public CountdownClock startCountdown(long fromMillis) {
		startCountdown(fromMillis, 0L);
		return this;
	}

	public CountdownClock startCountdown(long fromMillis, Long toMillis) {
		getState().setCounterStart(fromMillis);
		getState().setCounterTarget(toMillis);
		getState().setCounterDirection(Direction.DOWN);
		return this;
	}

	public CountdownClock startTimer() {
		startTimer(0);
		return this;
	}

	public CountdownClock startTimer(long fromMillis) {
		startTimer(fromMillis, null);
		return this;
	}

	public CountdownClock startTimer(long fromMillis, Long toMillis) {
		getState().setCounterStart(fromMillis);
		getState().setCounterTarget(toMillis);
		getState().setCounterDirection(Direction.UP);
		return this;
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
	 * Set the format for the clock. Available parameters:
	 * 
	 * %d - days %h - hours %m - minutes %s - seconds %ts - tenth of a seconds
	 * 
	 * For example "%d day(s) %h hour(s) and %m minutes" could produce the string "2
	 * day(s) 23 hour(s) and 5 minutes"
	 * 
	 * @param format
	 */
	public void setFormat(String format) {
		getState().setTimeFormat(format);
	}

	/**
	 * Set the format for the clock. Available parameters:
	 * 
	 * %d - days %h - hours %m - minutes %s - seconds %ts - tenth of a seconds
	 * 
	 * For example "%d day(s) %h hour(s) and %m minutes" could produce the string "2
	 * day(s) 23 hour(s) and 5 minutes"
	 * 
	 * @param format
	 */
	public CountdownClock withFormat(String format) {
		setFormat(format);
		return this;
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

	@Override
	public void beforeClientResponse(boolean initial) {
		if (getState().getCounterDirection() == Direction.UP
				&& getState().getCounterStart() > getState().getCounterTarget()) {
			throw new IllegalArgumentException("If direction is UP, counterStart should be lesl than counterTarget");
		}
		if (getState().getCounterDirection() == Direction.DOWN
				&& getState().getCounterStart() < getState().getCounterTarget()) {
			throw new IllegalArgumentException(
					"If direction is DOWN, counterStart should be greather than counterTarget");
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
