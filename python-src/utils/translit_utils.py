# from seq2seq.evaluators.reporter import get_decoded_words
from seq2seq.constants import EOS_token, SOS_token, STEP
from tools.amharic_to_cv import convert_amharic
import itertools
__author__ = 'Shyam'


def get_decoded_words(decoded_outputs):
    ans = []
    # print(decoded_outputs)
    for score, output in decoded_outputs:
        if output[-1] == EOS_token:
            output = output[:-1]
        if output[0] == SOS_token:
            output = output[1:]
        output = [p for p in output if p != STEP]
        output = " ".join(output)
        ans.append((score, output))
    # print(ans)
    return ans

# TODO Remove trailing ዊ,ን which are "-ian" marker
# TODO Dictionary east,west,south,north  (ደቡብ, dabube, south),
# TODO (ምዕራብ,meeraabe,west)  ("ንምብራቕ",nemeberaaqhe,east)
# TODO Checking ኣውሮፓ == አውሮፓ or ግሪክ == ግሪኽ
def get_transliterations(evaler, word):
    translits = []
    for tok in word.split(' '):
        input_word = " ".join(list(tok))
        input_word = convert_amharic(input_word)
        decoded_outputs = evaler.infer_on_example(sentence=input_word)
        scores_and_words = get_decoded_words(decoded_outputs)
        decoded_words = [w for s, w in scores_and_words]
        scores = [s for s, w in scores_and_words]
        ans = [w.replace(' ', '') for w in decoded_words]
        ans = [a for a in ans if len(a.strip()) > 0]
        translits.append(ans)
    return translits

def cross_product(lists):
    return itertools.product(*lists)