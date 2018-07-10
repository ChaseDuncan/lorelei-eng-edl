# Runs the end to end LORELEI English EDL
mvn compile
cpath="./target/dependency/*:./target/classes/"

LTF_DIR="/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/setE/eng/"
TA_DIR="/shared/preprocessed/cddunca2/lorelei/il10/cp2-setE/"

SUBMISSION="il10sub.tab"

java -classpath  ${cpath} -Xmx16g -Dfile.encoding=UTF-8 edu.illinois.cs.cogcomp.loreleiengedl.apps.LORELEI2018 ner \
    ${LTF_DIR} ${TA_DIR} 

#cd neural-el
#python neuralel_tadir.py --mode=ta --model_path=/shared/bronte/ngupta19/neural-el_resources/models/CDT.model \
#    --tadirpath=${TA_DIR}\
#    --taoutdirpath=${TA_DIR}
#
#cd ..
#
#java -classpath  ${cpath} -Xmx16g -Dfile.encoding=UTF-8 edu.illinois.cs.cogcomp.loreleiengedl.apps.LORELEI2018 ${LTF_DIR} ${TA_DIR} nomlink
#java -classpath  ${cpath} -Xmx16g -Dfile.encoding=UTF-8 edu.illinois.cs.cogcomp.loreleiengedl.apps.LORELEI2018 ${LTF_DIR} ${TA_DIR} submission \
#    ${SUBMISSION}
