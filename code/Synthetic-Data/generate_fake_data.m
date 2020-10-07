A = textread('sequences_test.txt', '%s', 'delimiter', ',');

for i=1:size(A,1)/6
    A{6*i} = round(normrnd(0,10,[1 100])); % Random Number generation
end


fileID = fopen('sequences.txt','w');

for i=1:size(A,1)
    if mod(i,6)==0
        for j=1:size(A{i},2)-1
            fprintf(fileID,'%d ',A{i}(j));
        end
        fprintf(fileID,'%d',A{i}(end));
        if mod(i,12)==0
            fprintf(fileID,'\n');
        else
            fprintf(fileID,',');
        end
    else
        fprintf(fileID,'%d',str2num(A{i}));
        fprintf(fileID,',');
    end
end