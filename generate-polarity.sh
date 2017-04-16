kotlin ./bin/predealingTools.jar  >  ./dataset/term_index.txt
kotlin ./bin/predealingTools.jar --svmIndexer > ./dataset/svm.fmt
./bin/train -s 0 ./dataset/svm.fmt
mv ./svm.fmt.model ./dataset/
./bin/predict ./dataset/svm.fmt ./dataset/svm.fmt.model ./dataset/result
kotlin ./bin/predealingTools.jar --weightChecker > polarity.txt
