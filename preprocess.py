import numpy as np
import codecs, sys, os, re, math, csv

from PorterStemmer import PorterStemmer
from bs4 import BeautifulSoup
from collections import Counter, OrderedDict

FILE_PATH = 'reuters21578/'


def main():
    stopwords = open('stoplist.txt','r').read().split()
    p = PorterStemmer()

    word_space = []
    id_class = {}
    id_content = {}

    for file in os.listdir(FILE_PATH):
        filename = os.fsdecode(file)

        if filename.endswith(".sgm"):
            print("Reading from: "+FILE_PATH+filename)
            f = codecs.open(FILE_PATH+filename, "r",encoding='utf-8', errors='ignore')
            soup = BeautifulSoup(f.read(), 'html.parser')   #BeautifulSoup4 automatically parse character entities to corresponding characters
            
            for article in soup.find_all('reuters'):

                topics = article.topics.contents

                if len(topics) == 1:
                    # Selecting the Subset of the Dataset for Clustering
                    ID = article.get('newid')
                    label = topics[0].contents[0]
                    content = article.body

                    if(content!=None):
                        content = content.contents[0].lower()           #Change the character case to lower-case.
                        content = re.sub('[^\x00-\x7F]+', ' ', content) #Eliminate any non-ascii characters.
                        content = re.sub('[^0-9a-zA-Z]+', ' ', content) #Replace any non alphanumeric characters with space.

                        '''
                        print()
                        print(ID)
                        print(label)
                        print(content.split())
                        '''

                        stem_word = []
                        for word in content.split():    #Split the text into tokens, using space as the delimiter.
                            #Eliminate any tokens that contain only digits and from the stop list that is provided.
                            if not word.isdigit() and not word in stopwords:
                                #Obtain the stem of each token using Porter's stemming algorithm(https://tartarus.org/martin/PorterStemmer/)   
                                w = p.stem(word, 0,len(word)-1)
                                stem_word.append(w)
                                word_space.append(w)

                            else:
                                continue

                        id_class[ID] = label
                        id_content[ID] = Counter(stem_word)
    
    sorted_id_class = OrderedDict(sorted(id_class.items(), key=lambda t: int(t[0])))
    
    print('Writing to: reuters21578.class')

    class_file = open('reuters21578.class', 'w')
    for k, v in sorted_id_class.items():
        class_file.write(k+','+v+'\n')
    class_file.close()


    #Eliminate any tokens that occur less than 5 times
    sorted_word_freq = OrderedDict((k,v) for k, v in sorted(Counter(word_space).items(), key=lambda t: t[1]) if v>=5)
    #print(sorted_word_freq)
    word_dimension = OrderedDict()
    dimension = 0

    for k,v in sorted_word_freq.items():
        word_dimension[k] = dimension
        dimension +=1

    #print(word_dimension)
    print('Writing to: reuters21578.clabel')
    clabel_file = open('reuters21578.clabel', 'w')
    for k, v in word_dimension.items():
        clabel_file.write(k+','+str(v)+'\n')
    clabel_file.close()

    # Vector Representations
    sorted_id_content = OrderedDict(sorted(id_content.items(), key=lambda t: int(t[0])))

    freq_csv = open('freq.csv', 'w')
    sqrtfreq_csv = open('sqrtfreq.csv', 'w')
    log2freq_csv = open('log2freq.csv', 'w')
    print('Writing to: freq.csv, sqrtfreq.csv, log2freq.csv')

    for k,words in sorted_id_content.items():
        for w,freq in words.items():
            if w in word_dimension:
                freq_csv.write(k+','+str(word_dimension[w])+','+str(freq)+'\n')

                sqrtfreq = 1+math.sqrt(freq)
                sqrtfreq_csv.write(k+','+str(word_dimension[w])+','+str(sqrtfreq)+'\n')

                log2freq = 1+math.log2(freq)
                log2freq_csv.write(k+','+str(word_dimension[w])+','+str(log2freq)+'\n')

    freq_csv.close()
    sqrtfreq_csv.close()
    log2freq_csv.close()


if __name__ == '__main__':
    main()