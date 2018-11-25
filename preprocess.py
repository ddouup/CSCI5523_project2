import numpy as np
import codecs, sys, os, re, html

from PorterStemmer import PorterStemmer
from bs4 import BeautifulSoup
from collections import Counter, OrderedDict

FILE_PATH = 'reuters21578/'


def main():
    stopwords = open('stoplist.txt','r').read().split()
    p = PorterStemmer()

    word_space = []
    articles = {}

    for file in os.listdir(FILE_PATH):
        filename = os.fsdecode(file)

        if filename.endswith(".sgm"):
            print("Reading from: "+FILE_PATH+filename)
            f = codecs.open(FILE_PATH+filename, "r",encoding='utf-8', errors='ignore')
            soup = BeautifulSoup(f.read(), 'html.parser')   #BeautifulSoup4 automatically parse character entities to corresponding characters
            
            for article in soup.find_all('reuters'):

                topics = article.topics.contents

                if len(topics) == 1:
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

    #word_space_count = {k: v for k, v in Counter(word_space).items() if v >= 5}
    #a = OrderedDict(sorted(word_space_count.items(), key=lambda t: -t[1]))

    word_space_count = OrderedDict((k,v) for k, v in sorted(Counter(word_space).items(),  key=lambda t: -t[1]) if v>=5) #Eliminate any tokens that occur less than 5 times.
    print(word_space_count)


if __name__ == '__main__':
    main()