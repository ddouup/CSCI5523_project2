import numpy as np
import codecs, sys, os

from PorterStemmer import PorterStemmer
from bs4 import BeautifulSoup

FILE_PATH = 'reuters21578/'

def main():

    for file in os.listdir(FILE_PATH):
        filename = os.fsdecode(file)
        if filename.endswith(".sgm"): 
            f = codecs.open(FILE_PATH+filename, "r",encoding='utf-8', errors='ignore')
            soup = BeautifulSoup(f.read(), 'html.parser')
            print(soup.prettify())
            print(f.read())
            sys.exit()
        else:
            continue

    '''
    stopwords = open('stoplist.txt','r')
    print(stopwords.read())
    p = PorterStemmer()
    '''



if __name__ == '__main__':
    main()