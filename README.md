# SLM: Structural Language Models of Code
This is an official implementation of the model described in:

"Structural Language Models of Code" [[PDF]](https://arxiv.org/pdf/1910.00577.pdf)

Appeared in ICML'2020.

An **online demo** is available at [https://AnyCodeGen.org](https://AnyCodeGen.org).

This repository currently contains the dataset and the data extractor that we used to create the Java dataset in the paper.


Feel free to open a [new issue](https://github.com/tech-srl/slm-code-generation/issues/new) 
for any question. We always respond quickly.

<center style="padding: 40px"><img width="70%" src="https://github.com/tech-srl/slm-code-generation/raw/master/images/fig1.png" /></center>
<center style="padding: 40px"><img width="70%" src="https://github.com/tech-srl/slm-code-generation/raw/master/images/fig2.png" /></center>


Table of Contents
=================
  * [Requirements](#requirements)
  * [Download our preprocessd dataset](#download-our-preprocessed-java-small-dataset)
  * [Creating a new dataset](#creating-and-preprocessing-a-new-java-dataset)
  * [Datasets](#datasets)
  * [Querying the trained model](#querying-the-trained-model)
  * [Citation](#citation)

## Requirements
  * [python3](https://www.linuxbabe.com/ubuntu/install-python-3-6-ubuntu-16-04-16-10-17-04) 
  * TensorFlow 1.13 or newer ([install](https://www.tensorflow.org/install/install_linux)). To check TensorFlow version:
> python3 -c 'import tensorflow as tf; print(tf.\_\_version\_\_)'
  * For [creating a new Java dataset](#creating-and-preprocessing-a-new-java-dataset): [JDK 12](https://openjdk.java.net/install/)



## Download our preprocessed Java-small dataset 
This dataset contains ~1.3M examples (1.1GB).
```
mkdir data
cd data
wget https://codegen-slm.s3.us-east-2.amazonaws.com/data/java-small-preprocessed.tar.gz
tar -xvzf java-small-preprocessed.tar.gz
```
This will create a `data/java-small/` sub-directory, containing the files that hold training, test and validation sets,
a dict file for various dataset properties and histograms, and a grammar file that is used during beam search to 
distinguish between terminal and non-terminal nodes.

## Creating and preprocessing a new Java dataset
To create and preprocess a new dataset (for example, to compare SLM to a new model on another dataset):
  * Edit the file [preprocess.sh](preprocess.sh) using the instructions there, pointing it to the correct training, validation and test directories.
  * Run the preprocess.sh file:
> bash preprocess.sh

## Datasets
### Java
To download the Java-small as raw `*.java` files, use:

  * [Java-small](https://s3.amazonaws.com/code2seq/datasets/java-small.tar.gz)
  
To download the preprocessed dataset, use:
  * [Java-small-preprocessed](https://codegen-slm.s3.us-east-2.amazonaws.com/data/java-small-preprocessed.tar.gz)

To download the dataset in a tokenized format that can be used in seq2seq models (for example, with [OpenNMT-py](http://opennmt.net/OpenNMT-py/)), use:
  * [Java-small-seq2seq](https://codegen-slm.s3.us-east-2.amazonaws.com/data/java-seq2seq-data.tar.gz)
  
The following JSON files are the files that are created by the JavaExtractor. 
The preprocessed and the seq2seq files
are created from these JSON files:
  * [Java-small-json](https://codegen-slm.s3.us-east-2.amazonaws.com/data/java-small-json.tar.gz)

Every line is a JSON object
that contains the following fields: `num_targets`, `num_nodes`, `targets`, 
`is_token`, `target_child_id`, `internal_paths`, `relative_paths`, `head_paths`, 
`head_root_path`, `head_child_id`, `linearized_tree`, `filepath`, `left_context`, 
`right_context`, `target_seq`, `line`.



### C#
The C# dataset that we used in the paper was created using the raw (`*.cs` files) dataset of
 [Allamanis et al., 2018](https://miltos.allamanis.com/publications/2018learning/),
 (https://aka.ms/iclr18-prog-graphs-dataset) and can be found here: [https://aka.ms/iclr18-prog-graphs-dataset](https://aka.ms/iclr18-prog-graphs-dataset).

To extract examples from the C# files, we modified the data extraction code of 
Brockschmidt et al., 2019: [https://github.com/microsoft/graph-based-code-modelling/](https://github.com/microsoft/graph-based-code-modelling/).

## Querying the Trained Model
To query the trained model, use the following API, where `MYCODE` is the given code snippet, that includes two question marks (`??`) to mark the "hole" that should be completed. 

### To query the expression-prediction model (the "paper model" in the demo website):
```
curl -X POST https://w0w3uc4a63.execute-api.us-east-1.amazonaws.com/prod/predict -d '{"code": "MYCODE"}'
```

For example:

```
curl -X POST https://w0w3uc4a63.execute-api.us-east-1.amazonaws.com/prod/predict -d '{"code": "public static Path[] stat2Paths(FileStatus[] stats) {  if (stats == null) return null;  Path[] ret = new Path[stats.length]; for (int i = 0; i < stats.length; ++i) { ret[i] = ??; } return ret; }"}'
```

### To query the statement-prediction model (the "extended model" in the demo website):
```
curl -X POST https://63g9yqims7.execute-api.us-east-1.amazonaws.com/prod/predict -d '{"code": "MYCODE"}'
```

For example:

```
curl -X POST https://63g9yqims7.execute-api.us-east-1.amazonaws.com/prod/predict -d '{"code": "@Override public boolean retainAll(Collection<?> collection) { boolean changed = false;     for (Iterator<E> iter = iterator(); iter.hasNext(); ) {         Element elem = iter.next();        if (!collection.contains(elem)) {           iter.remove();             ??        }    }    return changed;}"}'
```

## Citation 

[Structural Language Models of Code](https://arxiv.org/pdf/1910.00577.pdf)

```
@inproceedings{alon2020structural,
  title={Structural language models of code},
  author={Alon, Uri and Sadaka, Roy and Levy, Omer and Yahav, Eran},
  booktitle={International Conference on Machine Learning},
  pages={245--256},
  year={2020},
  organization={PMLR}
}
```
