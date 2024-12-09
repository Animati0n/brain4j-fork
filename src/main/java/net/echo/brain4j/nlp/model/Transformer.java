package net.echo.brain4j.nlp.model;

import net.echo.brain4j.layer.Layer;
import net.echo.brain4j.loss.LossFunctions;
import net.echo.brain4j.model.Model;
import net.echo.brain4j.model.initialization.WeightInitialization;
import net.echo.brain4j.nlp.model.layers.TransformerEncoder;
import net.echo.brain4j.training.data.DataSet;
import net.echo.brain4j.training.optimizers.Optimizer;
import net.echo.brain4j.utils.Vector;

import java.util.ArrayList;
import java.util.List;

public class Transformer extends Model {

    private Model concatModel;

    public Transformer(Layer... layers) {
        super(layers);
    }

    @Override
    public void compile(WeightInitialization type, LossFunctions function, Optimizer optimizer) {
        super.compile(type, function, optimizer);

        if (concatModel == null) return;

        concatModel.compile(type, function, optimizer);
    }

    public List<Vector> transform(List<Vector> embeddings) {
        List<Vector> resulting = new ArrayList<>(embeddings);

        for (Layer layer : layers) {
            if (layer instanceof TransformerEncoder encoder) {
                resulting = encoder.transform(resulting);
            }
        }

        for (Vector vector : resulting) {
            System.out.println("Resulting");
            System.out.println(vector);
        }

        List<Vector> concatEmbeddings = new ArrayList<>(resulting);

        for (Vector embedding : resulting) {
            concatEmbeddings.add(concatModel.predict(embedding));
        }

        return concatEmbeddings;
    }

    @Override
    public void fit(DataSet set, int batchSize) {

    }

    @Override
    public Vector predict(Vector input) {
        throw new UnsupportedOperationException("Transformer model is not supported for single input.");
    }

    public void concat(Layer... layers) {
        this.concatModel = new Model(layers);
    }
}
