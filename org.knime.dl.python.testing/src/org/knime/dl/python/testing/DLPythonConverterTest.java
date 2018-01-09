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
 * History
 *   Jun 28, 2017 (marcel): created
 */
package org.knime.dl.python.testing;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.dl.core.DLAbstractNetworkSpec;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultTensor;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLBuffer;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLReadableFloatBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.DLWritableDoubleBuffer;
import org.knime.dl.core.data.DLWritableFloatBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverter;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterRegistry;
import org.knime.dl.core.execution.DLAbstractExecutableNetwork;
import org.knime.dl.core.execution.DLAbstractExecutableNetworkAdapter;
import org.knime.dl.core.execution.DLExecutableNetwork;
import org.knime.dl.core.execution.DLExecutableNetworkAdapter;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.core.execution.DLKnimeNetworkExecutor;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.data.DLPythonDoubleBuffer;
import org.knime.dl.python.core.data.DLPythonFloatBuffer;
import org.knime.dl.python.core.data.DLPythonIntBuffer;
import org.knime.dl.python.core.data.DLPythonLongBuffer;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLPythonConverterTest {

	@Test
	public void testFooToBar() throws Exception {
		// register converters:
		DLDataValueToTensorConverterRegistry.getInstance()
				.registerConverter(new DLFooDataValueToFloatTensorConverterFactory());
		DLTensorToDataCellConverterRegistry.getInstance()
				.registerConverter(new DLDoubleBufferToBarDataCellConverterFactory());

		// network:

		final DLTensorSpec[] inputSpecs = new DLTensorSpec[1];
		inputSpecs[0] = new DLDefaultTensorSpec("in0", new DLDefaultFixedTensorShape(new long[] { 10, 10 }),
				float.class);
		final DLTensorSpec[] intermediateOutputSpecs = new DLTensorSpec[0];
		// intermediate outputs stay empty
		final DLTensorSpec[] outputSpecs = new DLTensorSpec[1];
		outputSpecs[0] = new DLDefaultTensorSpec("out0", new DLDefaultFixedTensorShape(new long[] { 10, 10 }),
				double.class);
		final DLBazNetworkSpec networkSpec = new DLBazNetworkSpec(inputSpecs, intermediateOutputSpecs, outputSpecs);
		final DLBazNetwork network = new DLBazNetwork(networkSpec, null);

		// input data:

		final Random rng = new Random(543653);
		final HashMap<String, DataCell[]> inputCells = new HashMap<>(network.getSpec().getInputSpecs().length);
		final FooDataCell[] input0Cells = new FooDataCell[1];
		for (int i = 0; i < input0Cells.length; i++) {
			final float[] arr = new float[10 * 10];
			for (int j = 0; j < arr.length; j++) {
				arr[j] = rng.nextFloat() * rng.nextInt(Short.MAX_VALUE);
			}
			input0Cells[i] = new FooDataCell(arr);
		}
		inputCells.put(network.getSpec().getInputSpecs()[0].getName(), input0Cells);

		// "configure":

		final DLBazExecutionContext exec = new DLBazExecutionContext();

		// input converters:
		final HashMap<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> inputConverters = new HashMap<>(
				networkSpec.getInputSpecs().length);
		for (final DLTensorSpec inputSpec : networkSpec.getInputSpecs()) {
			final DLDataValueToTensorConverterFactory<?, ?> converter = DLDataValueToTensorConverterRegistry
					.getInstance().getPreferredConverterFactory(FooDataCell.TYPE,
							exec.getTensorFactory().getWritableBufferType(inputSpec))
					.get();
			inputConverters.put(inputSpec, converter);
		}
		// output converters:
		final Map<DLTensorSpec, DLTensorToDataCellConverterFactory<?, ?>> outputConverters = new HashMap<>(
				networkSpec.getOutputSpecs().length + networkSpec.getHiddenOutputSpecs().length);
		for (final DLTensorSpec outputSpec : networkSpec.getOutputSpecs()) {
			final DLTensorToDataCellConverterFactory<?, ?> converter = DLTensorToDataCellConverterRegistry.getInstance()
					.getFactoriesForSourceType(exec.getTensorFactory().getReadableBufferType(outputSpec), outputSpec)
					.stream().filter(c -> {
						return c.getDestType().getCellClass() == BarDataCell.class;
					}).findFirst().get();
			outputConverters.put(outputSpec, converter);
		}
		for (final DLTensorSpec outputSpec : networkSpec.getHiddenOutputSpecs()) {
			final DLTensorToDataCellConverterFactory<?, ?> converter = DLTensorToDataCellConverterRegistry.getInstance()
					.getFactoriesForSourceType(exec.getTensorFactory().getReadableBufferType(outputSpec), outputSpec)
					.stream().filter(c -> c.getDestType().equals(BarDataCell.class)).findFirst().get();
			outputConverters.put(outputSpec, converter);
		}

		try (final DLKnimeNetworkExecutor knimeExec = new DLKnimeNetworkExecutor(
				exec.executable(network, outputConverters.keySet()), inputConverters, outputConverters)) {

			// "execute":

			// assign inputs to 'network input ports'/specs:
			final Map<DLTensorSpec, Iterable<DataValue>[]> inputs = new HashMap<>(inputConverters.size());
			for (final Entry<String, DataCell[]> input : inputCells.entrySet()) {
				final Optional<DLTensorSpec> inputSpec = Arrays.stream(network.getSpec().getInputSpecs())
						.filter(i -> i.getName().equals(input.getKey())).findFirst();
				final List<DataCell> val = Arrays.asList(input.getValue());
				inputs.put(inputSpec.get(), new Iterable[] { val });
			}

			final HashMap<DLTensorSpec, DataCell[]> outputs = new HashMap<>(outputConverters.size());

			knimeExec.execute(inputs, output -> {
				for (final Entry<DLTensorSpec, DataCell[][]> o : output.entrySet()) {
					DataCell[] dataCells = outputs.get(o.getKey());
					if (dataCells == null) {
						dataCells = o.getValue()[0];
					} else {
						dataCells = ArrayUtils.addAll(dataCells, o.getValue()[0]);
					}
					outputs.put(o.getKey(), dataCells);
				}
			}, null, 1);

			// check if conversion succeeded:
			Assert.assertEquals(outputs.size(), outputConverters.size());
			for (final Entry<DLTensorSpec, DLTensorToDataCellConverterFactory<?, ?>> outputSpecPair : outputConverters
					.entrySet()) {
				final Iterable<DataValue> inputsForSpec = inputs.get(networkSpec.getInputSpecs()[0])[0];
				final DataCell[] outputsForSpec = outputs.get(outputSpecPair.getKey());
				int i = 0;
				for (final DataValue input : inputsForSpec) {
					final DataCell output = outputsForSpec[i++];
					final float[] in = ((FooDataCell) input).getFloatArray();
					Assert.assertTrue(output instanceof BarDataCell);
					final double[] out = ((BarDataCell) output).getDoubleArray();
					for (int j = 0; j < out.length; j++) {
						Assert.assertEquals(out[j], in[j] * 5.0, 0.0);
					}
				}
			}
		}
	}

	static class DLBazNetwork implements DLPythonNetwork {

		private final URL m_source;

		private final DLBazNetworkSpec m_spec;

		private DLBazNetwork(final DLBazNetworkSpec spec, final URL source) {
			m_source = source;
			m_spec = spec;
		}

		@Override
		public URL getSource() {
			return m_source;
		}

		@Override
		public DLBazNetworkSpec getSpec() {
			return m_spec;
		}
	}

	static class DLBazNetworkSpec extends DLAbstractNetworkSpec {

		private static final long serialVersionUID = 1L;

		public DLBazNetworkSpec(final DLTensorSpec[] inputSpecs, final DLTensorSpec[] intermediateOutputSpecs,
				final DLTensorSpec[] outputSpecs) {
			super(inputSpecs, intermediateOutputSpecs, outputSpecs);
		}

		@Override
		protected void hashCodeInternal(final HashCodeBuilder b) {
			// no op
		}

		@Override
		protected boolean equalsInternal(final DLNetworkSpec other) {
			// no op
			return true;
		}
	}

	static class DLBazExecutionContext implements DLExecutionContext<DLBazNetwork> {

		private final DLTensorFactory m_layerDataFactory = new DLBazTensorFactory();

		@Override
		public Class<DLBazNetwork> getNetworkType() {
			return DLBazNetwork.class;
		}

		@Override
		public String getName() {
			return "Baz";
		}

		@Override
		public DLTensorFactory getTensorFactory() {
			return m_layerDataFactory;
		}

		@Override
		public DLExecutableNetworkAdapter executable(final DLBazNetwork network,
				final Set<DLTensorSpec> requestedOutputs) throws RuntimeException {
			final DLBazExecutableNetwork execNetwork = new DLBazExecutableNetwork(network);
			return new DLBazExecutableNetworkAdapter(execNetwork, m_layerDataFactory, requestedOutputs);
		}
	}

	static class DLBazExecutableNetwork extends DLAbstractExecutableNetwork<DLTensor<? extends DLWritableBuffer>, //
			DLTensor<? extends DLReadableBuffer>, DLBazNetwork> {

		public DLBazExecutableNetwork(final DLBazNetwork network) {
			super(network);
		}

		@Override
		public Class<?> getInputType() {
			return DLTensor.class;
		}

		@Override
		public Class<?> getOutputType() {
			return DLTensor.class;
		}

		@Override
		public void execute(final Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> input,
				final Map<DLTensorSpec, DLTensor<? extends DLReadableBuffer>> output, final long batchSize)
				throws Exception {
			// we fake some network activity here: unwrap floats, calc some
			// stuff, create doubles...
			for (final Entry<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> in : input.entrySet()) {
				// TODO: we can't be sure that casting will work here
				final DLPythonFloatBuffer buffer = (DLPythonFloatBuffer) in.getValue().getBuffer();
				final float[] inArr = buffer.getStorageForReading(0, buffer.size());
				final double[] outArr = new double[inArr.length];
				for (int i = 0; i < inArr.length; i++) {
					outArr[i] = inArr[i] * 5.0;
				}
				final DLTensorSpec outSpec = output.keySet().stream().findFirst().get();
				((DLWritableDoubleBuffer) output.get(outSpec).getBuffer()).putAll(outArr);
			}
		}

		@Override
		public void close() throws Exception {
			// here: no-op
		}
	}

	static class DLBazExecutableNetworkAdapter extends DLAbstractExecutableNetworkAdapter {

		protected DLBazExecutableNetworkAdapter(final DLExecutableNetwork<?, ?> network,
				final DLTensorFactory layerDataFactory, final Set<DLTensorSpec> requestedOutputs) {
			super(network, layerDataFactory, requestedOutputs);
		}

		@Override
		protected Map<DLTensorSpec, ?> extractNetworkInput(
				final Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> adapterInput) {
			return adapterInput;
		}

		@Override
		protected Map<DLTensorSpec, ?> extractNetworkOutput(
				final Map<DLTensorSpec, DLTensor<? extends DLReadableBuffer>> adapterOutput) {
			return adapterOutput;
		}
	}

	static class DLBazTensorFactory implements DLTensorFactory {

		@Override
		public Class<?> getNetworkType() {
			return DLBazNetwork.class;
		}

		@Override
		public Class<? extends DLWritableBuffer> getWritableBufferType(final DLTensorSpec spec) {
			final Class<?> t = spec.getElementType();
			if (t.equals(double.class)) {
				return DLWritableDoubleBuffer.class;
			} else if (t.equals(float.class)) {
				return DLWritableFloatBuffer.class;
			} else {
				throw new IllegalArgumentException("No matching buffer type.");
			}
		}

		@Override
		public Class<? extends DLReadableBuffer> getReadableBufferType(final DLTensorSpec spec) {
			final Class<?> t = spec.getElementType();
			if (t.equals(double.class)) {
				return DLReadableDoubleBuffer.class;
			} else if (t.equals(float.class)) {
				return DLReadableFloatBuffer.class;
			} else {
				throw new IllegalArgumentException("No matching buffer type.");
			}
		}

		@Override
		public DLTensor<? extends DLReadableBuffer> createReadableTensor(final DLTensorSpec spec, final long batchSize)
				throws IllegalArgumentException {
			return createTensorInternal(spec, batchSize, DLReadableBuffer.class);
		}

		@Override
		public DLTensor<? extends DLWritableBuffer> createWritableTensor(final DLTensorSpec spec, final long batchSize)
				throws IllegalArgumentException {
			return createTensorInternal(spec, batchSize, DLWritableBuffer.class);
		}

		private <B extends DLBuffer> DLTensor<B> createTensorInternal(final DLTensorSpec spec, final long batchSize,
				final Class<B> bufferType) {
			final long[] shape = DLUtils.Shapes.getFixedShape(spec.getShape())
					.orElseThrow(() -> new IllegalArgumentException(
							"Layer data spec does not provide a shape. Layer data cannot be created."));
			checkArgument(batchSize <= Integer.MAX_VALUE,
					"Invalid batch size. Factory only supports capacities up to " + Integer.MAX_VALUE + ".");
			final Class<?> t = spec.getElementType();
			final long size = DLUtils.Shapes.getSize(shape) * batchSize;
			final Supplier<B> s;
			if (t.equals(double.class)) {
				s = () -> (B) new DLPythonDoubleBuffer(size);
			} else if (t.equals(float.class)) {
				s = () -> (B) new DLPythonFloatBuffer(size);
			} else if (t.equals(int.class)) {
				s = () -> (B) new DLPythonIntBuffer(size);
			} else if (t.equals(long.class)) {
				s = () -> (B) new DLPythonLongBuffer(size);
			} else {
				throw new IllegalArgumentException("No matching tensor type.");
			}
			return new DLDefaultTensor<>(spec, s.get());
		}
	}

	static class DLFooDataValueToFloatTensorConverterFactory
			implements DLDataValueToTensorConverterFactory<FooDataValue, DLWritableFloatBuffer> {

		@Override
		public String getName() {
			return "From FooDataValue";
		}

		@Override
		public Class<FooDataValue> getSourceType() {
			return FooDataValue.class;
		}

		@Override
		public Class<DLWritableFloatBuffer> getBufferType() {
			return DLWritableFloatBuffer.class;
		}

		@Override
		public OptionalLong getDestCount(final List<DataColumnSpec> spec) {
			return OptionalLong.empty();
		}

		@Override
		public DLDataValueToTensorConverter<FooDataValue, DLWritableFloatBuffer> createConverter() {
			return (input, output) -> {
				final DLWritableFloatBuffer buf = output.getBuffer();
				buf.putAll(input.iterator().next().getFloatArray());
			};
		}
	}

	static class DLDoubleBufferToBarDataCellConverterFactory
			implements DLTensorToDataCellConverterFactory<DLReadableDoubleBuffer, BarDataCell> {

		@Override
		public String getName() {
			return "To BarDataCell";
		}

		@Override
		public Class<DLReadableDoubleBuffer> getBufferType() {
			return DLReadableDoubleBuffer.class;
		}

		@Override
		public DataType getDestType() {
			return DataType.getType(BarDataCell.class);
		}

		@Override
		public DLTensorToDataCellConverter<DLReadableDoubleBuffer, BarDataCell> createConverter() {
			return (exec, input, out) -> {
				final DLReadableDoubleBuffer buf = input.getBuffer();
				out.accept(new BarDataCell(buf.toDoubleArray()));
			};
		}

		@Override
		public long getDestCount(final DLTensorSpec spec) {
			return 1;
		}
	}

	static class FooDataCell extends DataCell implements FooDataValue {

		private static final long serialVersionUID = 1L;

		private static final DataType TYPE = DataType.getType(FooDataCell.class);

		private final float[] m_floats;

		private FooDataCell(final float[] floats) {
			m_floats = floats;
		}

		@Override
		public float[] getFloatArray() {
			return m_floats;
		}

		@Override
		public String toString() {
			return Arrays.toString(m_floats);
		}

		@Override
		protected boolean equalsDataCell(final DataCell dc) {
			return Arrays.equals(m_floats, ((FooDataCell) dc).m_floats);
		}

		@Override
		public int hashCode() {
			return m_floats.hashCode();
		}
	}

	static interface FooDataValue extends DataValue {
		float[] getFloatArray();
	}

	static class BarDataCell extends DataCell {

		private static final long serialVersionUID = 1L;

		private static final DataType TYPE = DataType.getType(BarDataCell.class);

		private final double[] m_doubles;

		private BarDataCell(final double[] doubles) {
			m_doubles = doubles;
		}

		private double[] getDoubleArray() {
			return m_doubles;
		}

		@Override
		public String toString() {
			return Arrays.toString(m_doubles);
		}

		@Override
		protected boolean equalsDataCell(final DataCell dc) {
			return Arrays.equals(m_doubles, ((BarDataCell) dc).m_doubles);
		}

		@Override
		public int hashCode() {
			return m_doubles.hashCode();
		}
	}
}
