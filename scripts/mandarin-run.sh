# Runs the end to end LORELEI English EDL

cpath="./target/dependency/*:./target/classes/"

java -classpath  ${cpath} -Xmx16g -Dfile.encoding=UTF-8 edu.illinois.cs.cogcomp.loreleiengedl.MandarinLRLPTest ner

cd neural-el
python neuralel_tadir.py --mode=ta --model_path=/shared/bronte/ngupta19/neural-el_resources/models/CDT.model \
    --tadirpath=/shared/experiments/cddunca2/mandarin-text-annotation-json/ \
    --taoutdirpath=/shared/experiments/cddunca2/mandarin-text-annotation-json/

cd ..

java -classpath  ${cpath} -Xmx16g -Dfile.encoding=UTF-8 edu.illinois.cs.cogcomp.loreleiengedl.MandarinLRLPTest nomlink
java -classpath  ${cpath} -Xmx16g -Dfile.encoding=UTF-8 edu.illinois.cs.cogcomp.loreleiengedl.MandarinLRLPTest submission
