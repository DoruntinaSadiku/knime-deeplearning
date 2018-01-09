/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.dl.keras.base.nodes.learner;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.LocalDateTime;

import org.knime.core.node.ExecutionContext;
import org.knime.dl.keras.base.nodes.learner.view.DLProgressMonitor;
import org.knime.dl.keras.base.nodes.learner.view.DLViewData;
import org.knime.dl.keras.core.training.DLKerasTrainingMonitor;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasDefaultTrainingMonitor implements DLKerasTrainingMonitor, DLProgressMonitor {

	private int m_numEpochs;

	private int m_numBatchesPerEpoch;

	private boolean m_isRunning;

	private boolean m_hasData;

	private boolean m_hasStoppedEarly;
	
	private boolean m_hasLearningFinished;

	private LocalDateTime m_startTime;

	private LocalDateTime m_endTime;

	private int m_currentEpoch;

	private int m_currentBatchInEpoch;

	private String[] m_metricsNames;

	private float[] m_metrics;

	private DLViewData<?>[] m_viewData;

	private ExecutionContext m_exec;

	private Runnable m_onTrainingStartCallback;

	private Runnable m_onTrainingEndCallback;

	private Runnable m_onBatchEndCallback;

	@Override
	public int getNumEpochs() {
		return m_numEpochs;
	}

	@Override
	public int getNumBatchesPerEpoch() {
		return m_numBatchesPerEpoch;
	}

	@Override
	public boolean isRunning() {
		return m_isRunning;
	}

	@Override
	public void setIsRunning(final boolean isRunning) {
		m_isRunning = isRunning;
	}

	@Override
	public boolean hasData() {
		return m_hasData;
	}

	@Override
	public void setHasData(final boolean hasData) {
		m_hasData = hasData;
	}

	@Override
	public LocalDateTime getStartDateTime() {
		return m_startTime;
	}

	@Override
	public LocalDateTime getEndDateTime() {
		return m_endTime;
	}

	@Override
	public int getCurrentEpoch() {
		return m_currentEpoch;
	}

	@Override
	public void setCurrentEpoch(final int currentEpoch) {
		m_currentEpoch = currentEpoch;
	}

	@Override
	public int getCurrentBatchInEpoch() {
		return m_currentBatchInEpoch;
	}

	@Override
	public void setCurrentBatchInEpoch(final int currentBatchInEpoch) {
		m_currentBatchInEpoch = currentBatchInEpoch;
	}

	@Override
	public String[] getMetricsNames() {
		return m_metricsNames;
	}

	@Override
	public void setMetricsNames(final String[] names) {
		m_metricsNames = names;
	}

	@Override
	public float[] getCurrentMetrics() {
		return m_metrics;
	}

	@Override
	public void setCurrentMetrics(final float[] metrics) {
		m_metrics = metrics;
	}

	@Override
	public DLViewData<?>[] getDataUpdate() {
		return m_viewData;
	}

	@Override
	public void setDataUpdate(final DLViewData<?>[] viewData) {
		m_viewData = viewData;
	}

	@Override
	public ExecutionContext getExecutionContext() {
		return m_exec;
	}

	@Override
	public void onTrainingStart(final Runnable callback) {
		m_onTrainingStartCallback = callback;
	}

	@Override
	public void notifyTrainingStart() {
		if (m_onTrainingStartCallback != null) {
			m_onTrainingStartCallback.run();
		}
	}

	@Override
	public boolean hasStoppedEarly() {
		return m_hasStoppedEarly;
	}

	@Override
	public void setHasStoppedEarly(final boolean hasStoppedEarly) {
		m_hasStoppedEarly = hasStoppedEarly;
	}
	
	@Override
	public boolean hasLearningFinished() {
		return m_hasLearningFinished;
	}

	@Override
	public void onTrainingEnd(final Runnable callback) {
		m_onTrainingEndCallback = callback;
	}

	@Override
	public void notifyTrainingEnd() {
		if (m_onTrainingEndCallback != null) {
			m_onTrainingEndCallback.run();
		}
	}

	@Override
	public void onBatchEnd(final Runnable callback) {
		m_onBatchEndCallback = callback;
	}

	@Override
	public void notifyBatchEnd() {
		if (m_onBatchEndCallback != null) {
			m_onBatchEndCallback.run();
		}
	}

	@Override
	public void writeExternal(final ObjectOutput objOut) throws IOException {
		objOut.writeInt(m_numEpochs);
		objOut.writeInt(m_numBatchesPerEpoch);
		objOut.writeBoolean(m_isRunning);
		objOut.writeBoolean(m_hasData);
		objOut.writeObject(m_startTime);
		objOut.writeObject(m_endTime);
		objOut.writeInt(m_currentEpoch);
		objOut.writeInt(m_currentBatchInEpoch);
		objOut.writeObject(m_metricsNames);
		objOut.writeObject(m_metrics);
		objOut.writeBoolean(m_hasStoppedEarly);
		objOut.writeBoolean(m_hasLearningFinished);
	}

	@Override
	public void readExternal(final ObjectInput objIn) throws IOException, ClassNotFoundException {
		m_numEpochs = objIn.readInt();
		m_numBatchesPerEpoch = objIn.readInt();
		m_isRunning = objIn.readBoolean();
		m_hasData = objIn.readBoolean();
		m_startTime = (LocalDateTime) objIn.readObject();
		m_endTime = (LocalDateTime) objIn.readObject();
		m_currentEpoch = objIn.readInt();
		m_currentBatchInEpoch = objIn.readInt();
		m_metricsNames = (String[]) objIn.readObject();
		m_metrics = (float[]) objIn.readObject();
		m_hasStoppedEarly = objIn.readBoolean();
		m_hasLearningFinished = objIn.readBoolean();
	}

	void setNumEpochs(final int numEpochs) {
		m_numEpochs = numEpochs;
	}

	void setNumBatchesPerEpoch(final int numBatchesPerEpoch) {
		m_numBatchesPerEpoch = numBatchesPerEpoch;
	}

	void setStartTime(final LocalDateTime startTime) {
		m_startTime = startTime;
	}

	void setEndTime(final LocalDateTime endTime) {
		m_endTime = endTime;
	}

	void setExecutionContext(final ExecutionContext exec) {
		m_exec = exec;
	}

	void setHasLearningFinished(boolean hasFinished) {
		m_hasLearningFinished = hasFinished;		
	}
}
