/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * History
 *   May 29, 2017 (marcel): created
 */
package org.knime.dl.core.data.convert;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.dl.core.DLAbstractExtensionPointRegistry;
import org.knime.dl.core.data.DLWritableBuffer;

/**
 * Registry for deep learning input converter factories that allow conversion of {@link DataValue data values} into
 * {@link DLWritableBuffer layer data buffer} types.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLDataValueToLayerDataConverterRegistry extends DLAbstractExtensionPointRegistry {

	private static final String EXT_POINT_ID = "org.knime.dl.DLDataValueToLayerDataConverterFactory";

	private static final String EXT_POINT_ATTR_CLASS = "DLDataValueToLayerDataConverterFactory";

	private static DLDataValueToLayerDataConverterRegistry instance;

	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static synchronized DLDataValueToLayerDataConverterRegistry getInstance() {
		if (instance == null) {
			instance = new DLDataValueToLayerDataConverterRegistry();
		}
		return instance;
	}

	private final HashMap<String, DLDataValueToLayerDataConverterFactory<?, ?>> m_converters = new HashMap<>();

	/**
	 * Creates a new registry instance.
	 */
	private DLDataValueToLayerDataConverterRegistry() {
		super(EXT_POINT_ID, EXT_POINT_ATTR_CLASS);
		register();
	}

	// access methods:

	/**
	 * Returns all deep learning {@link DLDataValueToLayerDataConverterFactory converter factories} that create
	 * converters which convert a specific source type into a specific destination buffer.
	 *
	 * @param sourceType the source type
	 * @param bufferType the destination type
	 * @return all deep learning converter factories that allow conversion of the source type into the destination type
	 */
	public final List<DLDataValueToLayerDataConverterFactory<? extends DataValue, ?>> getConverterFactories(
			final DataType sourceType, final Class<? extends DLWritableBuffer> bufferType) {
		final HashSet<DLDataValueToLayerDataConverterFactory<?, ?>> convs = new HashSet<>();
		for (final DLDataValueToLayerDataConverterFactory<?, ?> candidate : m_converters.values()) {
			if (candidate.getBufferType().isAssignableFrom(bufferType)
					&& sourceType.isCompatible(candidate.getSourceType())) {
				convs.add(candidate);
			}
		}
		if (sourceType.isCollectionType()) {
			for (final DLDataValueToLayerDataConverterFactory<? extends DataValue, ?> conv : getConverterFactories(
					sourceType.getCollectionElementType(), bufferType)) {
				convs.add(new DLCollectionDataValueToLayerDataConverterFactory<>(conv));
			}
		}
		return convs.stream().sorted(Comparator.comparing(DLDataValueToLayerDataConverterFactory::getIdentifier))
				.collect(Collectors.toList());
	}

	/**
	 * Returns the preferred deep learning {@link DLDataValueToLayerDataConverterFactory converter factory} that creates
	 * converters which convert a specific source type into a specific destination buffer.
	 *
	 * @param sourceType the source type
	 * @param bufferType the destination type
	 * @return the preferred deep learning converter factory that allows conversion of the source type into the
	 *         destination type
	 */
	public final Optional<DLDataValueToLayerDataConverterFactory<? extends DataValue, ?>> getPreferredConverterFactory(
			final DataType sourceType, final Class<? extends DLWritableBuffer> bufferType) {
		final List<DLDataValueToLayerDataConverterFactory<? extends DataValue, ?>> convs =
				getConverterFactories(sourceType, bufferType);
		DLDataValueToLayerDataConverterFactory<?, ?> sourceMatch = null;
		final DataType theSourceType =
				sourceType.isCollectionType() ? sourceType.getCollectionElementType() : sourceType;
		for (final DLDataValueToLayerDataConverterFactory<? extends DataValue, ?> conv : convs) {
			final Class<? extends DataValue> theConvSourceType;
			if (conv instanceof DLCollectionDataValueToLayerDataConverterFactory) {
				final DLCollectionDataValueToLayerDataConverterFactory<?, ?> casted =
						(DLCollectionDataValueToLayerDataConverterFactory<?, ?>) conv;
				theConvSourceType = casted.getSourceElementType();
			} else {
				theConvSourceType = conv.getSourceType();
			}
			if (theSourceType.getPreferredValueClass().equals(theConvSourceType)) {
				if (conv.getBufferType() == bufferType) {
					return Optional.of(conv);
				}
				if (sourceMatch == null || sourceMatch.getBufferType().isAssignableFrom(conv.getBufferType())) {
					sourceMatch = conv;
				}
			}
		}
		return sourceMatch != null ? Optional.of(sourceMatch) : convs.stream().findFirst();
	}

	/**
	 * Returns the deep learning {@link DLDataValueToLayerDataConverterFactory converter factory} with the given
	 * identifier if present.
	 *
	 * @param identifier the identifier of the converter factory
	 * @return the deep learning converter factory that matches the identifier
	 */
	public final Optional<DLDataValueToLayerDataConverterFactory<? extends DataValue, ?>> getConverterFactory(
			final String identifier) {
		if (identifier == null || identifier.isEmpty()) {
			return Optional.empty();
		}
		if (identifier.startsWith(DLCollectionDataValueToLayerDataConverterFactory.class.getName())) {
			final String elementConverterId =
					identifier.substring(DLCollectionDataValueToLayerDataConverterFactory.class.getName().length() + 1,
							identifier.length() - 1);
			final Optional<DLDataValueToLayerDataConverterFactory<?, ?>> conv = getConverterFactory(elementConverterId);
			if (conv.isPresent()) {
				return Optional.of(new DLCollectionDataValueToLayerDataConverterFactory<>(conv.get()));
			} else {
				return Optional.empty();
			}
		}
		return Optional.ofNullable(m_converters.get(identifier));
	}
	// :access methods

	// registration:

	/**
	 * Registers the given converter factory.
	 *
	 * @param converter the converter factory to register
	 * @throws IllegalArgumentException if a converter factory with the same identifier is already registered or if the
	 *             given converter factory's identifier or name is null or empty
	 */
	public final void registerConverter(final DLDataValueToLayerDataConverterFactory<?, ?> converter)
			throws IllegalArgumentException {
		registerConverterInternal(converter);
	}

	@Override
	protected void registerInternal(final IConfigurationElement elem, final Map<String, String> attrs)
			throws Throwable {
		registerConverterInternal(
				(DLDataValueToLayerDataConverterFactory<?, ?>) elem.createExecutableExtension(EXT_POINT_ATTR_CLASS));
	}

	private synchronized void registerConverterInternal(final DLDataValueToLayerDataConverterFactory<?, ?> converter) {
		final String id = converter.getIdentifier();
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("The converter factory's id must be neither null nor empty.");
		}
		if (m_converters.containsKey(id)) {
			throw new IllegalArgumentException("A converter factory with id '" + id + "' is already registered.");
		}
		m_converters.put(id, converter);
	}
	// :registration
}
