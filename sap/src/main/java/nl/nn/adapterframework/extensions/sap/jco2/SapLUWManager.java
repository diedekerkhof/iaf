/*
   Copyright 2013, 2020 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.extensions.sap.jco2;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineExitHandler;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;

/**
 * Manager for SAP Logical Units of Work (LUWs). 
 * Used to begin, commit or rollback LUWs. A SapLUWManager can be placed before a number
 * of SapSenders. The SapLUWManager and the SapSenders must each use the same value for
 * luwHandleSessionKey. By doing so, they use the same connection to SAP. This allows to
 * perform a commit on a number of actions.<br>
 * The placement of the the first SapLUWManager is optionan: By specifying a new 
 * luwHandleSessionKey a new handle is created implicitly.<br>
 * To explicityly commit or rollback a set of actions, a SapLUWManager-pipe can be used, with 
 * the action-attribute set apropriately.
 * 
 * @author  Gerrit van Brakel
 * @since   4.6.0
 */
public class SapLUWManager extends FixedForwardPipe implements IPipeLineExitHandler {

	public static final String ACTION_BEGIN="begin";
	public static final String ACTION_COMMIT="commit";
	public static final String ACTION_ROLLBACK="rollback";
	public static final String ACTION_RELEASE="release";

	private @Getter String luwHandleSessionKey;
	private @Getter String action;
	private @Getter String sapSystemName;
	
	private SapSystem sapSystem;


	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getAction())) {
			throw new ConfigurationException("action should be specified, it must be one of: "+
				ACTION_BEGIN+", "+ACTION_COMMIT+", "+ACTION_ROLLBACK+", "+ACTION_RELEASE+".");
		}
		if (!getAction().equalsIgnoreCase(ACTION_BEGIN) &&
			!getAction().equalsIgnoreCase(ACTION_COMMIT) &&
			!getAction().equalsIgnoreCase(ACTION_ROLLBACK) &&
			!getAction().equalsIgnoreCase(ACTION_RELEASE)) {
			throw new ConfigurationException("illegal action ["+getAction()+"] specified, it must be one of: "+
				ACTION_BEGIN+", "+ACTION_COMMIT+", "+ACTION_ROLLBACK+", "+ACTION_RELEASE+".");
		}
		if (getAction().equalsIgnoreCase(ACTION_BEGIN)) {
			getPipeLine().registerExitHandler(this);
		}
		if (StringUtils.isEmpty(getLuwHandleSessionKey())) {
			throw new ConfigurationException("action should be specified, it must be one of: "+
				ACTION_BEGIN+", "+ACTION_COMMIT+", "+ACTION_ROLLBACK+", "+ACTION_RELEASE+".");
		}
		sapSystem=SapSystem.getSystem(getSapSystemName());
		if (sapSystem==null) {
			throw new ConfigurationException(getLogPrefix(null)+"cannot find SapSystem ["+getSapSystemName()+"]");
		}
	}

	@Override
	public void atEndOfPipeLine(String correlationId, PipeLineResult pipeLineResult, PipeLineSession session) throws PipeRunException {
		SapLUWHandle.releaseHandle(session,getLuwHandleSessionKey());
	}

	public void open() throws SenderException {
		try {
			sapSystem.openSystem();
		} catch (SapException e) {
			close();
			throw new SenderException(getLogPrefix(null)+"exception starting SapSender", e);
		}
	}
	
	public void close() {
		sapSystem.closeSystem();
	}


	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (getAction().equalsIgnoreCase(ACTION_BEGIN)) {
			SapLUWHandle.retrieveHandle(session,getLuwHandleSessionKey(),true,getSapSystem(),false).begin();
		} else
		if (getAction().equalsIgnoreCase(ACTION_COMMIT)) {
			SapLUWHandle handle=SapLUWHandle.retrieveHandle(session,getLuwHandleSessionKey());
			if (handle==null) {
				throw new PipeRunException(this, "commit: cannot find handle under sessionKey ["+getLuwHandleSessionKey()+"]");
			}
			handle.commit();
		} else
		if (getAction().equalsIgnoreCase(ACTION_ROLLBACK)) {
			SapLUWHandle handle=SapLUWHandle.retrieveHandle(session,getLuwHandleSessionKey());
			if (handle==null) {
				throw new PipeRunException(this, "rollback: cannot find handle under sessionKey ["+getLuwHandleSessionKey()+"]");
			}
			handle.rollback();
		} else {
			if (getAction().equalsIgnoreCase(ACTION_RELEASE)) {
				SapLUWHandle.releaseHandle(session,getLuwHandleSessionKey());
			}
		} 
		return new PipeRunResult(getSuccessForward(),message);
	}



	public SapSystem getSapSystem() {
		return sapSystem;
	}




	@IbisDoc({"1", "name of the SapSystem used by this object", ""})
	public void setSapSystemName(String string) {
		sapSystemName = string;
	}

	@IbisDoc({"2", "one of: begin, commit, rollback, release", ""})
	public void setAction(String string) {
		action = string;
	}

	@IbisDoc({"3", "session key under which information is stored", ""})
	public void setLuwHandleSessionKey(String string) {
		luwHandleSessionKey = string;
	}

}
