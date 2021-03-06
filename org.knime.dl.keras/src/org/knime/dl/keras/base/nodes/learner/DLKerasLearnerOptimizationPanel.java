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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXCollapsiblePane.Direction;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.nodes.AbstractGridBagDialogComponentGroup;
import org.knime.dl.base.nodes.DialogComponentObjectSelection;
import org.knime.dl.base.nodes.IDialogComponentGroup;
import org.knime.dl.base.settings.ConfigEntry;
import org.knime.dl.base.settings.ConfigUtil;
import org.knime.dl.keras.core.training.DLKerasOptimizer;
import org.knime.dl.keras.core.training.DLKerasTrainingContext;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasLearnerOptimizationPanel extends AbstractGridBagDialogComponentGroup {

	private final DLKerasLearnerGeneralConfig m_cfg;

	private final DialogComponentObjectSelection<DLKerasOptimizer> m_dcOptimizer;

	private final JXCollapsiblePane m_optimizerParamGroupWrapper;

	DLKerasLearnerOptimizationPanel(final DLKerasLearnerGeneralConfig cfg) {
		m_cfg = cfg;

		// optimizer selection
		final ConfigEntry<DLKerasOptimizer> optimizer = m_cfg.getOptimizerEntry();
		m_dcOptimizer = new DialogComponentObjectSelection<>(optimizer, DLKerasOptimizer::getName, "Optimizer");
		addDoubleColumnRow(getFirstComponent(m_dcOptimizer, JLabel.class),
				getFirstComponent(m_dcOptimizer, JComboBox.class));

		// optimizer parameters
		m_optimizerParamGroupWrapper = new JXCollapsiblePane(Direction.UP);
		m_optimizerParamGroupWrapper.setAnimated(false);
		addComponent(m_optimizerParamGroupWrapper);
		optimizer.addValueChangeListener((entry, oldValue) -> updateOptimizerPanel(entry.getValue()));

		// clip norm
		final ConfigEntry<Double> clipNorm = m_cfg.getClipNormEntry();
		addToggleNumberEditRowComponent(clipNorm, "Clip norm", ConfigUtil.toSettingsModelDouble(clipNorm));

		// clip value
		final ConfigEntry<Double> clipValue = m_cfg.getClipValueEntry();
		addToggleNumberEditRowComponent(clipValue, "Clip value", ConfigUtil.toSettingsModelDouble(clipValue));
	}

	@Override
	public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		m_cfg.copyClipSettingsToOptimizer();
	}

	@Override
	public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {
		refreshAvailableOptimizers();
	}

	private void updateOptimizerPanel(final DLKerasOptimizer opti) {
		m_cfg.copyClipSettingsToOptimizer();
		// display the parameter group of the currently selected optimizer
		m_optimizerParamGroupWrapper.setCollapsed(true);
		final IDialogComponentGroup optimizerParamGroup = opti.getParameterDialogGroup();
		m_optimizerParamGroupWrapper.removeAll();
		m_optimizerParamGroupWrapper.add(optimizerParamGroup.getComponentGroupPanel());
		m_optimizerParamGroupWrapper.setCollapsed(false);
	}

	void refreshAvailableOptimizers() throws NotConfigurableException {
		// refresh available optimizers
		final DLKerasTrainingContext<?> selectedTrainingContext = m_cfg.getContextEntry().getValue();
		if (selectedTrainingContext == null) {
			throw new NotConfigurableException("There is no available back end that supports the input network.");
		}
		final List<DLKerasOptimizer> availableOptimizers = selectedTrainingContext.createOptimizers().stream() //
				.sorted(Comparator.comparing(DLKerasOptimizer::getName)) //
				.collect(Collectors.toList());
		if (availableOptimizers.isEmpty()) {
			throw new NotConfigurableException("There is no available optimizer that supports the input network.");
		}
		final DLKerasOptimizer selectedOptimizer = m_cfg.getOptimizerEntry().getValue() != null
				? m_cfg.getOptimizerEntry().getValue()
				: availableOptimizers.get(0);
		for (int i = availableOptimizers.size() - 1; i >= 0; i--) {
			if (availableOptimizers.get(i).getClass() == selectedOptimizer.getClass()) {
				availableOptimizers.remove(i);
				availableOptimizers.add(i, selectedOptimizer);
			}
		}
		m_dcOptimizer.replaceListItems(availableOptimizers, selectedOptimizer);
		updateOptimizerPanel(selectedOptimizer);
	}
}
