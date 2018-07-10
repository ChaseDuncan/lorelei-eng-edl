# Runs the end to end LORELEI English EDL

TA_IN_DIR="/shared/preprocessed/cddunca2/lorelei/il9/"
TA_OUT_DIR="/shared/preprocessed/cddunca2/lorelei/il9/el-only-test/"

cd neural-el
python neuralel_tadir.py --mode=ta --model_path=/shared/bronte/ngupta19/neural-el_resources/models/CDT.model \
    --tadirpath=${TA_IN_DIR}\
    --taoutdirpath=${TA_OUT_DIR}

cd ..

