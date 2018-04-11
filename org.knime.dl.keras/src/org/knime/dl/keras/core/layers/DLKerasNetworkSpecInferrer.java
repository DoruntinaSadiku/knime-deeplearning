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
package org.knime.dl.keras.core.layers;

import java.util.ArrayList;
import java.util.List;

import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.DLKerasGenericNetworkSpec;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator.DLKerasLayerVisitor;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator.DLNetworkLayerGraphTraversalException;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkSpecInferrer {

    private DLKerasNetworkLayerNameGenerator m_layerNameGen;

    /**
     * Infers the specification of the Keras network graph specified by the given output layers and their parents (i.e.
     * predecessor nodes).
     *
     * @param outputLayers the output layers of the network whose spec to infer
     * @return the inferred network spec
     * @throws DLNetworkLayerGraphTraversalException if traversing the network graph failed
     */
    public DLKerasNetworkSpec inferNetworkSpec(final List<DLKerasLayer> outputLayers) {
        m_layerNameGen = new DLKerasNetworkLayerNameGenerator();
        final List<DLTensorSpec> inputSpecs = new ArrayList<>(5);
        final List<DLTensorSpec> hiddenSpecs = new ArrayList<>(20);
        final List<DLTensorSpec> outputSpecs = new ArrayList<>(5);
        new DLKerasNetworkLayerGraphIterator(outputLayers).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                outputSpecs.addAll(amendTensorIdsAndNames(outputLayer, outputLayer.getOutputSpecs()));
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                hiddenSpecs.addAll(amendTensorIdsAndNames(hiddenLayer, hiddenLayer.getOutputSpecs()));
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                inputSpecs.addAll(amendTensorIdsAndNames(inputLayer, inputLayer.getInputSpecs()));
            }
        });
        return new DLKerasGenericNetworkSpec(inputSpecs.toArray(new DLTensorSpec[0]),
            hiddenSpecs.toArray(new DLTensorSpec[0]), outputSpecs.toArray(new DLTensorSpec[0]));
    }

    private List<DLTensorSpec> amendTensorIdsAndNames(final DLKerasLayer layer, final List<DLTensorSpec> tensorSpecs) {
        final List<DLTensorSpec> amendedTensorSpecs = new ArrayList<>(tensorSpecs.size());
        for (int i = 0; i < tensorSpecs.size(); i++) {
            final DLTensorSpec tensorSpec = tensorSpecs.get(i);
            final DLTensorSpec amendedTensorSpec;
            final String layerName = m_layerNameGen.getNextLayerName(layer);
            // TODO: add support for Keras layer nodes
            final String tensorName = m_layerNameGen.getOutputTensorName(layerName, 0, i);
            if (tensorSpec.getBatchSize().isPresent()) {
                amendedTensorSpec = new DLDefaultTensorSpec(new DLDefaultTensorId(tensorName), tensorName,
                    tensorSpec.getBatchSize().getAsLong(), tensorSpec.getShape(), tensorSpec.getElementType(),
                    tensorSpec.getDimensionOrder());
            } else {
                amendedTensorSpec = new DLDefaultTensorSpec(new DLDefaultTensorId(tensorName), tensorName,
                    tensorSpec.getShape(), tensorSpec.getElementType(), tensorSpec.getDimensionOrder());
            }
            amendedTensorSpecs.add(amendedTensorSpec);
        }
        return amendedTensorSpecs;
    }
}