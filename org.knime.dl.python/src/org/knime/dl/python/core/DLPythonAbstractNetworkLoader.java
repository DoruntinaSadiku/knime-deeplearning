/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.dl.python.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLInvalidDestinationException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLMissingDependencyException;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLPythonAbstractNetworkLoader<N extends DLPythonNetwork> implements DLPythonNetworkLoader<N> {

	private static DLPythonInstallationTestResults previousTestResults;

	protected abstract DLPythonAbstractCommands createCommands(DLPythonContext context)
			throws DLInvalidEnvironmentException;

	@Override
	public synchronized void checkAvailability(final boolean forceRefresh) throws DLMissingDependencyException {
		if (forceRefresh || previousTestResults == null) {
			previousTestResults = new DLPythonInstallationTestResults();
			try (DLPythonDefaultContext context = new DLPythonDefaultContext()) {
				createCommands(context).testInstallation();
				previousTestResults.m_success = true;
			} catch (final DLInvalidEnvironmentException e) {
				previousTestResults.m_success = false;
				previousTestResults.m_message = e.getMessage();
			}
		}
		if (!previousTestResults.m_success) {
			throw new DLMissingDependencyException(previousTestResults.m_message);
		}
	}

	@Override
	public void save(final DLPythonNetworkHandle handle, final URL destination, final DLPythonContext context)
			throws IllegalArgumentException, DLInvalidDestinationException, DLInvalidEnvironmentException, IOException {
		validateDestination(destination);
		final File destinationFile = FileUtil.getFileFromURL(destination);
		final DLPythonAbstractCommands commands = createCommands(checkNotNull(context));
		commands.saveNetwork(checkNotNull(handle), destinationFile.getAbsolutePath());
	}

	protected static final class DLPythonInstallationTestResults {

		protected boolean m_success;

		protected String m_message;
	}
}
