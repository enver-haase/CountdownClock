package org.vaadin.kim.countdownclock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.vaadin.kim.countdownclock.client.ui.CountdownClockClientRpc;
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

	private final CountdownClockClientRpc clientRpc;
	protected List<EndEventListener> listeners = new ArrayList<EndEventListener>();
	private boolean running = false;
	private Long initialTime;

	public CountdownClock() {
		clientRpc = getRpcProxy(CountdownClockClientRpc.class);
		CountdownClockRpc rpc = new CountdownClockRpc() {
			private static final long serialVersionUID = -7392569455421206075L;

			public void countdownEnded() {
				if (!getState().isContinueAfterEnd()) {
					running = false;
				}
				for (EndEventListener listener : listeners) {
					listener.countDownEnded(CountdownClock.this);
				}
			}
		};
		registerRpc(rpc);
	}

	public CountdownClock(String format) {
		this();
		setFormat(format);
	}

	@Override
	protected CountdownClockState getState() {
		return (CountdownClockState) super.getState();
	}

	public void setTime(long millis) {
		initialTime = millis;
		clientRpc.setTime(millis);
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

	public CountdownClock start() {
		clientRpc.start();
		running = true;
		return this;
	}

	public CountdownClock start(long startMillis, long targetMillis) {
		setTime(startMillis);
		setTargetTime(targetMillis);
		return start();
	}

	public CountdownClock start(long startMillis, Direction direction) {
		setTime(startMillis);
		setDirection(direction);
		return start();
	}

	public void stop() {
		running = false;
		clientRpc.stop();
	}

	private boolean isRunning() {
		return running;
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
	 * <ul>
	 * <li>%- minus sign if the time is negative or empty string</li>
	 * <li>%d days</li>
	 * <li>%h hours</li>
	 * <li>%m minutes</li>
	 * <li>%M minutes with 2 digits</li>
	 * <li>%s seconds</li>
	 * <li>%S seconds with 2 digits</li>
	 * <li>%ts tenth of a seconds</li>
	 * </ul>
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

	public CountdownClock autostart(boolean autostart) {
		setAutostart(autostart);
		return this;
	}
	
	public boolean isAutostart() {
		return getState().isAutostart();
	}

	public void setAutostart(boolean autostart) {
		getState().setAutostart(autostart);
	}

	@Override
	public void beforeClientResponse(boolean initial) {
		if (getState().isAutostart() || isRunning()) {
			if (getFormat() == null) {
				throw new IllegalStateException("Should set a format");
			}
			if (getDirection() == null) {
				if (getTargetTime() == null) {
					throw new IllegalStateException("Eiter TargetTime or Direction must be set");
				}
				if(initialTime == null) {
					throw new IllegalStateException("You must callSetTime before start()");
				}
				if (initialTime < getTargetTime()) {
					setDirection(Direction.UP);
				} else {
					setDirection(Direction.DOWN);
				}
			} else if (getDirection() == Direction.UP && initialTime > getTargetTime()) {
				throw new IllegalArgumentException(
						"If direction is UP, counterStart should be lesl than counterTarget");
			} else if (getDirection() == Direction.DOWN && initialTime < getTargetTime()) {
				throw new IllegalArgumentException(
						"If direction is DOWN, counterStart should be greather than counterTarget");
			}
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
