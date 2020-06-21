#!/usr/bin/env bash
###########################################################
# Change the following values to preprocess a new dataset.
# TRAIN_DIR, VAL_DIR and TEST_DIR should be paths to      
#   directories containing sub-directories with .java files
# DATASET_NAME is just a name for the currently extracted 
#   dataset.                                              
# MAX_CONTEXTS is the number of contexts to keep in the dataset for each 
#   method (by default 1000). At training time, these contexts
#   will be downsampled dynamically.
# SUBTOKEN_VOCAB_SIZE, TARGET_VOCAB_SIZE -   
#   - the number of subtokens and target words to keep 
#   in the vocabulary (the top occurring words and paths will be kept). 
# NUM_THREADS - the number of parallel threads to use. It is 
#   recommended to use a multi-core machine for the preprocessing 
#   step and set this value to the number of cores.
# PYTHON - python3 interpreter alias.
DATASET_DIR=JavaExtractor/JPredict/src/test/TestData
#DATASET_DIR=JavaExtractor/JPredict/src/test/TestLongPaths
#DATASET_DIR=JavaExtractor/JPredict/src/test/TestAnnotations
#DATASET_DIR=~/Allamanis-dataset/java-small
#DATASET_DIR=/scratch/data/java1000/java-med/
#DATASET_DIR=/scratch/data/java2/java-large/
TRAIN_DIR=${DATASET_DIR} #/training
VAL_DIR=${DATASET_DIR} #/validation
TEST_DIR=${DATASET_DIR} #/test
#DATASET_NAME=java-small-if-root_max200_json
DATASET_NAME=test_json
MAX_CONTEXTS=100
MAX_INTERNAL_PATHS=5
MAX_PATH_LENGTH=12
MAX_RELATIVE_PATH_LENGTH=6
MAX_PATH_WIDTH=3
MAX_CHILD_ID=5
SUBTOKEN_VOCAB_SIZE=190000
NUM_THREADS=64
EXP=''
#EXP='--exp'
FILTERREP='--filterrep'
PYTHON=python3
###########################################################

TRAIN_DATA_FILE=${DATASET_NAME}.train.raw.json
VAL_DATA_FILE=${DATASET_NAME}.val.raw.json
TEST_DATA_FILE=${DATASET_NAME}.test.raw.json
GRAMMAR_FILE=${DATASET_NAME}.grammar.gen
EXTRACTOR_JAR=JavaExtractor/JPredict/target/JavaExtractor-0.0.1-SNAPSHOT.jar

mkdir -p data
mkdir -p data/${DATASET_NAME}

echo "Extracting paths from validation set..."
${PYTHON} JavaExtractor/extract.py --dir ${VAL_DIR} \
    --max_path_length ${MAX_PATH_LENGTH} --max_path_width ${MAX_PATH_WIDTH} --max_child_id ${MAX_CHILD_ID} \
    --max_internal_paths ${MAX_INTERNAL_PATHS} ${EXP} \
    --num_threads ${NUM_THREADS} --jar ${EXTRACTOR_JAR} --json_max_contexts ${MAX_CONTEXTS} | shuf > ${VAL_DATA_FILE} 2>> error_log.txt
echo "Finished extracting paths from validation set"
echo "Extracting paths from test set..."
${PYTHON} JavaExtractor/extract.py --dir ${TEST_DIR} \
    --max_path_length ${MAX_PATH_LENGTH} --max_path_width ${MAX_PATH_WIDTH} --max_child_id ${MAX_CHILD_ID} \
    --max_internal_paths ${MAX_INTERNAL_PATHS} ${EXP} \
    --num_threads ${NUM_THREADS} --jar ${EXTRACTOR_JAR} --json_max_contexts ${MAX_CONTEXTS} | shuf > ${TEST_DATA_FILE} 2>> error_log.txt
echo "Finished extracting paths from test set"
echo "Extracting paths from training set..."
${PYTHON} JavaExtractor/extract.py --dir ${TRAIN_DIR} \
    --max_path_length ${MAX_PATH_LENGTH} --max_path_width ${MAX_PATH_WIDTH} --max_child_id ${MAX_CHILD_ID} \
    --max_internal_paths ${MAX_INTERNAL_PATHS} ${EXP} \
    --num_threads ${NUM_THREADS} --jar ${EXTRACTOR_JAR} --json_max_contexts ${MAX_CONTEXTS} | shuf > ${TRAIN_DATA_FILE} 2>> error_log.txt
echo "Finished extracting paths from training set"

${PYTHON} scripts/collect_grammar.py --json ${TRAIN_DATA_FILE} --output data/${DATASET_NAME}/${GRAMMAR_FILE}

#${PYTHON} scripts/filter_tests_and_rep.py --json ${TEST_DATA_FILE} --output ${TEST_DATA_FILE}-filtered ${FILTERREP}
#${PYTHON} scripts/filter_tests_and_rep.py --json ${VAL_DATA_FILE} --output ${TEST_DATA_FILE}-filtered ${FILTERREP}
#${PYTHON} scripts/filter_tests_and_rep.py --json ${TRAIN_DATA_FILE} --output ${TRAIN_DATA_FILE}-filtered ${FILTERREP}

#${PYTHON} preprocess.py --train_data ${TRAIN_DATA_FILE}-filtered --test_data ${TEST_DATA_FILE}-filtered --val_data ${VAL_DATA_FILE}-filtered \

${PYTHON} preprocess.py --train_data ${TRAIN_DATA_FILE} --test_data ${TEST_DATA_FILE} --val_data ${VAL_DATA_FILE} \
  --max_contexts ${MAX_CONTEXTS} --subtoken_vocab_size ${SUBTOKEN_VOCAB_SIZE} \
  --max_relative_path_length ${MAX_RELATIVE_PATH_LENGTH} --max_internal_paths ${MAX_INTERNAL_PATHS} \
  --output_name data/${DATASET_NAME}/${DATASET_NAME} --max_path_length ${MAX_PATH_LENGTH} \
  --max_path_width ${MAX_PATH_WIDTH} --max_child_id ${MAX_CHILD_ID}

# If all went well, the raw data files can be deleted, because preprocess.py creates new files 
# with truncated and padded number of paths for each example.
#rm ${TRAIN_DATA_FILE} ${VAL_DATA_FILE} # ${TEST_DATA_FILE}

