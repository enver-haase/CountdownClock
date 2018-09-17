package org.vaadin.kim.countdownclock.client.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

public class VCountdownClock extends Widget {

	public interface CountdownEndedListener {
		public void countdownEnded();
	}

	public static enum Direction {
		UP, DOWN
	}

	/** Set the tagname used to statically resolve widget from UIDL. */
	public static final String TAGNAME = "countdownclock";

	/** Set the CSS class name to allow styling. */
	public static final String CLASSNAME = "v-" + TAGNAME;

	public static final String CLASSNAME_OVERTIME = CLASSNAME + "-overtime";

	private long startTime;
	private long ticksCount;

	private long time = 0;
	private Long endTime;
	private boolean wasOvertime = false;

	protected Counter counter = new Counter();

	protected List<CountdownEndedListener> listeners = new ArrayList<VCountdownClock.CountdownEndedListener>();

	protected Direction direction = Direction.DOWN;
	protected int timerInterval = 1000;

	/**
	 * The constructor should first call super() to initialize the component and
	 * then handle any initialization relevant to Vaadin.
	 */
	public VCountdownClock() {
		setElement(Document.get().createDivElement());
		// This method call of the Paintable interface sets the component
		// style name in DOM tree
		setStyleName(CLASSNAME);
	}

	private boolean continueAfterEnd = false;

	private TimeStringBuilder timeStringBuilder;

	protected void setTimeFormat(String format) {

		timeStringBuilder = new TimeStringBuilder(format);
		Integer precision = timeStringBuilder.getPrecision();
		if (precision != null) {
			timerInterval = precision;
		} else {
			timerInterval = 1000;
		}
	}

	private String format(long time) {
		if (time < 0) {
			time = time * -1;
		}
		return timeStringBuilder.format(time);
	}

	public void startClock() {
		startTime = new Date().getTime();
		ticksCount = 0;

		counter.scheduleRepeating(timerInterval);
		counter.run();
	}

	protected void updateLabel() {
		getElement().setInnerHTML(format(getTime()));
		// right now wasOvertime is equal to getOvertime()
		if (wasOvertime == true) {
			addStyleName(CLASSNAME_OVERTIME);
		} else {
			removeStyleName(CLASSNAME_OVERTIME);
		}
	}

	protected class Counter extends Timer {
		@Override
		public void run() {
			// can't trust the timer precision
			long realTime = new Date().getTime() - startTime;
			long idealTime = timerInterval * ticksCount;
			int drift = (int) (realTime - idealTime);

			setTime(getTime() + ((direction == Direction.UP ? 1 : -1) * timerInterval));

			ticksCount++;
			counter.scheduleRepeating(timerInterval - drift);

			if (!getContinueAfterEnd() && isOvertime()) {
				cancel();
				setTime(getEndTime());
			}
		}
	}

	@Override
	protected void onDetach() {
		super.onDetach();
		counter.cancel();
	}

	public void fireEndEvent() {
		for (CountdownEndedListener listener : listeners) {
			listener.countdownEnded();
		}
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
		boolean overtime = isOvertime();
		if (overtime && !wasOvertime) {
			// only the first time overtime is true
			fireEndEvent();
		}
		wasOvertime = overtime;
		updateLabel();
	}

	public boolean isOvertime() {
		return getEndTime() != null && ((direction == Direction.UP && getTime() >= getEndTime())
				|| (direction == Direction.DOWN && getTime() <= getEndTime()));
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long counterTarget) {
		this.endTime = counterTarget;
	}

	public boolean getContinueAfterEnd() {
		return continueAfterEnd;
	}

	public void addListener(CountdownEndedListener listener) {
		listeners.add(listener);
	}

	public void setContinueAfterEnd(boolean continueAfterEnd) {
		this.continueAfterEnd = continueAfterEnd;
	}

	public void stop() {
		counter.cancel();
	}
}
