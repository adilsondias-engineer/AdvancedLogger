package au.com.apiled.module.api.component;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.mule.runtime.extension.api.annotation.param.Parameter;
/**
 * Basic attributes implementation that defines a generic {@link #toString()}
 * method.
 * 
 * @since 4.0
 */
public class AdvancedLoggerBaseAttributes implements Serializable {

		
	private static final long serialVersionUID = 134755L;
	@Parameter
	private Date start;
	@Parameter
	private Date end;
	@Parameter
	private String transactionId;
	@Parameter
	private String event;
	
	@Parameter
	private String action;

	@Parameter
	private long timeTaken;
	@Parameter
	private HashMap<String,String> customProperties = new HashMap<String,String>();
	//= new HashMap<String,String>();
	
	private Date startSubflow;
	private Date endSubflow;
	
	public Date getStartSubflow() {
		return startSubflow;
	}

	public void setStartSubflow(Date startSubflow) {
		this.startSubflow = startSubflow;
	}

	public Date getEndSubflow() {
		return endSubflow;
	}

	public void setEndSubflow(Date endSubflow) {
		this.endSubflow = endSubflow;
	}

	public HashMap<String, String> getCustomProperties() {
		return customProperties;
	}

	public void setCustomProperties(HashMap<String, String> customProperties2) {
		this.customProperties = customProperties2;
	}

	public long getTimeTaken() {
		return timeTaken;
	}

	public void setTimeTaken(long timeTaken) {
		this.timeTaken = timeTaken;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
	}
}
