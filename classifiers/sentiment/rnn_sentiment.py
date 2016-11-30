import numpy as np, pandas as pd, os, re, string, cPickle, tensorflow as tf
from random import shuffle

from itertools import izip_longest

np.random.seed(123)

a = None

FLAGS = tf.app.flags.FLAGS
tf.app.flags.DEFINE_string('mode', 'train', """train or evaluate.""")
tf.app.flags.DEFINE_string('sentence', '', '')


def create_word_vecs(sentences, lower=True, min_freq=4):
    """ Creates word vectors using a corpus of sentences.
    ATTENTION: Always remember to create word vectors from the training set
        ONLY. Never use the dev or test sets to create word vectors.

    Creates a numpy ndarray with ndim=2, where each line is a word vector from
        a word in a corpus. Each vector is initialized by drawing from a
        gaussian distribution with mean 0 and std 0.1

    This function also creates word vectors for the tokens `MASK` and `UNK`,
        for masking indexes and unknown words, respectively.

    :param sentences: List of list of strings, i.e. a list of tokenized
        sentences.
    :param lower: If the words should be converted to their lower case
        counterpart. Default=True
    :param min_freq: The minimum number of occurrences that a word should have
        for a word vector to be created. Default=4

    :returns: A tuple, where the first element is a numpy matrix with each word
        vector created and the second element is a `word2index` dict. This dict
        is indexed by the words and returns their index in the word vectos
        matrix.
    """
    words = {}

    for sentence in sentences:
        for word in sentence:
            if lower:
                word = word.lower()
            if word not in words:
                words[word] = 0
            words[word] += 1

    words_to_use = []
    for word, count in words.items():
        if count < min_freq:
            continue
        words_to_use.append(word)

    words = words_to_use

    word_vecs = np.random.normal(loc=0.0, scale=.1, size=(len(words) + 2, 30))
    word2index = {}
    word2index['MASK'] = 0 #For Keras masking
    word2index['UNK'] = 1 #Out of vocabulary words

    for word in words:
        word2index[word] = len(word2index)

    return word_vecs.astype('float32'), word2index

def encode_sentences(sentences, word2index):
    """ Encodes all sentences in a tokenized corpus using a word2index dict.

    :param sentences: List of list of strings, i.e. a list of tokenized
        sentences.
    :param word2index: Dict with tokens as keys and indexes as values

    :returns: List of list of ints, i.e. the translated corpus
    """
    translated = []

    for sentence in sentences:
        translated_sentence = []
        for word in sentence:
            word = word.lower()
            if word not in word2index:
                word = 'UNK'
            translated_sentence.append(word2index[word])
        translated.append(translated_sentence)
    return translated


def to_np_ndarray(data):
    """ Takes an encoded corpus and transforms it into a numpy ndarray, padding
        if necessary
    """

    def find_shape(seq):
        try:
            len_ = len(seq)
        except TypeError:
            return ()
        shapes = [find_shape(subseq) for subseq in seq]
        return (len_,) + tuple(max(sizes) for sizes in izip_longest(*shapes,
                                                                    fillvalue=1))

    def fill_array(arr, seq, val=0):
        if arr.ndim == 1:
            try:
                len_ = len(seq)
            except TypeError:
                len_ = 0
            arr[:len_] = seq
            arr[len_:] = val
        else:
            for subarr, subseq in izip_longest(arr, seq, fillvalue=()):
                fill_array(subarr, subseq)

    padded_data = np.empty(shape=find_shape(data))
    fill_array(padded_data, data)

    return padded_data.astype('int32')

def encode_labels(data):
    """ Takes a list of strings (labels) and builds a matrix of one-hot vectors
            and a label2index dict
    """
    label2index = {}
    encoded_labels = []
    for label in data:
        if label not in label2index:
            label2index[label] = len(label2index)
        encoded_labels.append(label2index[label])

    onehot_labels = np.zeros(shape=(len(data), len(label2index)), dtype='int32')
    onehot_labels[np.arange(len(data)), encoded_labels] = 1

    return onehot_labels, label2index

def clean_tweet(tweet):
    #Convert to lower case
    tweet = tweet.lower()
    #Convert www.* or https?://* to URL
    tweet = re.sub('((www\.[^\s]+)|(https?://[^\s]+))','URL',tweet)
    #Convert @username to AT_USER
    tweet = re.sub('@[^\s]+','ATUSER',tweet)
    #Remove additional white spaces
    tweet = re.sub('[\s]+', ' ', tweet)
    #Replace #word with word
    tweet = re.sub(r'#([^\s]+)', r'\1', tweet)
    #trim
    tweet = tweet.strip('\'"')
    # Remove ,'.?!
    regex = re.compile('[%s]' % re.escape(string.punctuation))
    tweet = regex.sub('', tweet)
    # Convert numbers to NUMBERRR
    tweet = re.sub(" (-?\d+)|(\+1) ", ' NUMBERRR ', tweet)
    return tweet
  
def prepare_Sentiment140(num_class_examples):
    # Train data
    positive_start = 800001
    train_data_negative = pd.read_csv('training.1600000.processed.noemoticon.csv', 
                                      header=None, delimiter=",", nrows=num_class_examples)
    train_data_positive = pd.read_csv('training.1600000.processed.noemoticon.csv', 
                                      header=None, delimiter=",", nrows=num_class_examples, skiprows=positive_start-1)
    train_data = []
    for i in range(0, len(train_data_positive[0])):
      train_data.append([clean_tweet(train_data_negative[5][i]), 0])
      train_data.append([clean_tweet(train_data_positive[5][i]), 1])
    shuffle(train_data)
    
    # Test data
    test_data = []
    test_data_both = pd.read_csv('testdata.manual.2009.06.14.csv', header=None, delimiter=",")
    for i in range(0, len(test_data_both[0])):
      if test_data_both[0][i] in (0,4):
        test_data.append([clean_tweet(test_data_both[5][i]), 0 if test_data_both[0][i]==0 else 1])
    
    cPickle.dump([train_data, test_data], open("tweets_clean_{}.pkl".format(num_class_examples), "wb"))

def read_data(num_class_examples):
    f = file("tweets_clean_{}.pkl".format(num_class_examples), 'rb')
    train_data, test_data = cPickle.load(f)          # these 3 lines loads the file from disk
    f.close() 
    
    train_sentences = [x[0].split() for x in train_data]
    train_labels = [x[1] for x in train_data]
    test_sentences = [x[0].split() for x in test_data]
    test_labels = [x[1] for x in test_data]
    
#     #PUT YOUR READING AND TOKENIZING CODE HERE
#     train_sentences = [
#         ['a', 'sentence'],
#         ['yet', 'another', 'sentence']
#     ]
#     train_labels = ['a_sentence', 'another_sentence']
# 
#     test_sentences = []
#     test_labels = []

    return train_sentences, train_labels, test_sentences, test_labels
  
def build_model_tf(inputs, word_vecs):
    recurrent_dim = 512
    n_classes = 2
    
    # Create the embedding layer
    embeddings = tf.Variable(word_vecs, name='embedding')
    embed = []
    for i in range(0, inputs.get_shape()[1].value):
        embed.append(tf.nn.embedding_lookup(embeddings, inputs[:,i]))
    
    # Create the Recurrent layer(s)
    with tf.variable_scope('rec_cell', initializer=tf.random_normal_initializer(0.0, 0.1)):
        cell = tf.nn.rnn_cell.GRUCell(num_units=recurrent_dim)
        outputs, state = tf.nn.rnn(cell, embed, dtype=tf.float32)
    
    # Compute the logits
    W = tf.Variable(tf.zeros([recurrent_dim, n_classes]), name='logits_w')
    b = tf.Variable(tf.zeros([n_classes]), name='logits_b')
    logits = tf.matmul(outputs[-1], W) + b
    
    return logits
  
def one_sentence():
    # Load the word2index mapping
    f = file('word2index.pkl', 'rb')
    word2index, max_words_in_sentence, word_vecs = cPickle.load(f)
    f.close()
    
    # Create the inference model
    # Create the placeholders
    inputs = tf.placeholder(tf.int32, (None, max_words_in_sentence), name='inputs')

    # Create the model
    print 'Creating the computation graph'
    logits = build_model_tf(inputs, word_vecs)
    probs = tf.nn.softmax(logits)
    
    # Restore model
    sess = tf.Session()
    saver = tf.train.Saver(tf.all_variables())
    saver.restore(sess, os.getcwd() + '/model.sv')
    
    # Get output
    sentence = FLAGS.sentence.split()
    sentence = encode_sentences([sentence], word2index)
    sentence = to_np_ndarray(sentence)
    sentence = np.pad(sentence, [(0,0),(0,max_words_in_sentence - sentence.shape[1])], 'constant')
    probs_numpy = sess.run(probs, feed_dict={inputs:sentence})
    print probs_numpy[0][1]

def new_data():
    # Load the word2index mapping
    f = file('word2index.pkl', 'rb')
    word2index, max_words_in_sentence, word_vecs = cPickle.load(f)
    f.close()
    
    # Create the inference model
    # Create the placeholders
    inputs = tf.placeholder(tf.int32, (None, max_words_in_sentence), name='inputs')

    # Create the model
    print 'Creating the computation graph'
    logits = build_model_tf(inputs, word_vecs)
    probs = tf.nn.softmax(logits)
    
    sess = tf.Session()
    saver = tf.train.Saver(tf.all_variables())
    saver.restore(sess, os.getcwd() + '/model.sv')
    print ''
    user_input = raw_input("Enter your own text (q to quit): \n")
    while user_input != 'q':
        sentence = user_input.split()
        sentence = encode_sentences([sentence], word2index)
        sentence = to_np_ndarray(sentence)
        sentence = np.pad(sentence, [(0,0),(0,max_words_in_sentence - sentence.shape[1])], 'constant')
        probs_numpy = sess.run(probs, feed_dict={inputs:sentence})
        print 'Negative: {:.2f}%, Positive: {:.2f}%'.format(probs_numpy[0][0]*100, probs_numpy[0][1]*100)
        print ''
        user_input = raw_input("Enter your own text (q to quit): \n")

def save_model(sess, saver, word2index, max_words_in_sentence, word_vecs):
    saver.save(sess, os.getcwd() + '/model.sv')
    cPickle.dump([word2index, max_words_in_sentence, word_vecs], open("word2index.pkl", "wb"))

def main():
    num_class_examples = 500000
    lr = 0.1
    n_steps = 100000000
    mb = 100
    gc = 1.0
    print_every = 200
    save_every = 2000
    
    # Get the data, prepare it, and create the word embedding initial matrix
    train_sentences, train_labels, test_sentences, test_labels = read_data(num_class_examples)
    word_vecs, word2index = create_word_vecs(train_sentences)
    train_sentences = encode_sentences(train_sentences, word2index)
    test_sentences = encode_sentences(test_sentences, word2index)
    all_sentences = to_np_ndarray(train_sentences + test_sentences)
    train_sentences = all_sentences[0:len(train_sentences)]
    test_sentences = all_sentences[-len(test_sentences):]
#     labels, label2index = encode_labels(train_labels + test_labels)
#     train_labels = labels[:len(train_labels)]
#     test_labels = labels[len(train_labels):]
    max_words_in_sentence = train_sentences.shape[1]
    
    
    # For debug
    global a
    a = train_sentences

    # Create the placeholders
    inputs = tf.placeholder(tf.int32, (None, max_words_in_sentence), name='inputs')
    labels = tf.placeholder(tf.int32, name='labels')

    # Create the model
    print 'Creating the computation graph'
    global_step = tf.Variable(0, trainable=False) 
    logits = build_model_tf(inputs, word_vecs)
    
    # Compute the cost and accuracy
    cross_entropy = tf.nn.sparse_softmax_cross_entropy_with_logits(
      logits, labels, name='cross_entropy_per_example')
    cross_entropy_mean = tf.reduce_mean(cross_entropy, name='cross_entropy')
    top_k_op = tf.reduce_mean(tf.to_float(tf.nn.in_top_k(logits, labels, 1)))
    
    # Create the optimizer
    print 'Creating the optimization graph nodes'
    opt = tf.train.GradientDescentOptimizer(lr)
    grads = opt.compute_gradients(loss=cross_entropy_mean, var_list=tf.trainable_variables())
    for i in range(0,len(grads)):
        grads[i] = (tf.clip_by_norm(grads[i][0], gc), grads[i][1])
    apply_gradient_op = opt.apply_gradients(grads, global_step=global_step)
    
    # Init vars, create session, saver
    saver = tf.train.Saver(tf.trainable_variables())
    sess = tf.Session()
    sess.run(tf.initialize_all_variables())
    
    # The training loop
    print 'Training...'
    curr = 0
    
    # Init aggregators
    cost_sum = 0
    acc_sum = 0
    
    for i in range(1, n_steps):
        # Get the minibatch
        batch_inputs = train_sentences[curr:curr+mb]
        batch_labels = train_labels[curr:curr+mb]
        
        # process one minibatch
        _, c, acc = sess.run([apply_gradient_op, cross_entropy_mean, top_k_op], feed_dict={inputs: batch_inputs, labels: batch_labels})
        
        # Add to aggregators
        cost_sum += c
        acc_sum += acc
    
        # Increase curr
        curr = (curr + mb) % (2 * num_class_examples)
        
        # Do some printing
        if i % print_every == 0:
            # Print train values
            print 'After {} training batches:'.format(i)
            print 'Train cost: {}'.format(cost_sum / float(print_every))
            print 'Train accuracy: {}'.format(acc_sum / float(print_every))
            
            # Set aggregators to 0
            cost_sum = 0
            acc_sum = 0
            
            # Get test results
            test_acc = sess.run(top_k_op, feed_dict={inputs: test_sentences, labels: test_labels})
            print 'Test accuracy: {}'.format(test_acc)
            print ''
        
        # Save the model
        if i % save_every == 0:
            save_model(sess, saver, word2index, max_words_in_sentence, word_vecs)

if __name__ == "__main__":
    if FLAGS.mode == 'train':
        #main()
	print 'To not overwrite the current model, training is not allowed here'
    elif FLAGS.mode == 'live':
        new_data()
    if FLAGS.mode == 'sentence':
        one_sentence()
    elif FLAGS.mode == 'prepare':
        #prepare_Sentiment140(500000)
	'To not overwrite the current model, this is not allowed here'
