% Check the quality of word/sentence embeddings

clear;
dimension = 30;
filename = [pwd '/temp_results/sentence_vectors.txt'];

%get_sorted(filename, dimension);

vec = get_word2vec_matrix(filename, dimension); % The third argument indicates normalization option.

%write_normalized_word2vec_data(filename, vec, dimension); 

%output similar sentences
test_id = 12;
text = textread([pwd '/temp_results/recon_tweet.txt'], '%s', 'delimiter', '\n');
sim = vec(test_id,:)*vec';
[~,b] = sort(sim, 'descend');
fileID = fopen([pwd '/temp_results/similar_tweets.txt'],'w');
fprintf(fileID,'%s',text{test_id});
fprintf(fileID,'\n\n');
for i=1:50
    fprintf(fileID,'%s',text{b(i)});
    fprintf(fileID,'\n');
end
fclose(fileID);


function vec = get_word2vec_matrix(filename, dimension)
text = textread(filename, '%s', 'delimiter', ' ');
vec = zeros(length(text)/(dimension+1),dimension);
for i=1:length(text)/(dimension+1)
    for j=1:dimension
        vec(i,j) = str2double(text{(i-1)*(dimension+1)+1+j});
    end
end
% Check normalization
for i=1:size(vec,1)
    if abs(norm(vec(i,:))-1) > 0.0000001
        disp([num2str(i) 'th vector has NOT been normalized!'])
    end
    %vec(i,:) = vec(i,:)/norm(vec(i,:));
end

end

function write_normalized_word2vec_data(filename, vec, dimension)
% Write to file the normalized word2vec data
if exist([filename(1:end-4) '_sorted.txt'], 'file')==2
    text = textread([filename(1:end-4) '_sorted.txt'], '%s', 'delimiter', ' ');
else
    text = textread(filename, '%s', 'delimiter', ' ');
end
fileID = fopen([filename(1:end-4) '_normalized.txt'],'w');
temp = vec';
count = 0;
for i=1:length(text)
    if mod((i-1),(dimension+1))==0
        count = count+1;
        if (i~=1)
            fprintf(fileID,'\n');
        end
        fprintf(fileID,'%s ',text{i});
    else
        fprintf(fileID,'%s ',num2str(temp(i-count)));
    end
end
fclose(fileID);
end

function output_similar_sentence(sim, test_id, text)
[~,b] = sort(sim(test_id,:), 'descend');
fileID = fopen('similar_tweets.txt','w');
fprintf(fileID,'%s',text{test_id});
fprintf(fileID,'\n\n');
for i=1:50
    fprintf(fileID,'%s',text{b(i)});
    fprintf(fileID,'\n');
end
fclose(fileID);
end

function get_sorted(filename, dimension)
text = textread(filename, '%s', 'delimiter', ' ');
for i=1:length(text)/(dimension+1)
    text_seq(i) = str2num(text{(i-1)*(dimension+1)+1}(14:end));
end
[~,seq] = sort(text_seq); % seq~[1, max]
text = textread(filename, '%s', 'delimiter', ',');
fileID = fopen([filename(1:end-4) '_sorted.txt'],'w');
for i=1:size(text,1)
    fprintf(fileID,'%s',text{seq(i)});
    fprintf(fileID,'\n');
end
fclose(fileID);
end
