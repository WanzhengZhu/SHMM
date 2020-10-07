clear;
text = textread('synthetic_data.txt', '%s', 'delimiter', ',');

N = 2; % Length of the HMM
RandVMF = [];
for i=1:size(text,1)
    if mod(i,6*N)==0
        RandVMF = [RandVMF; str2num(text{i})];
    end
end
figure;
scatter3(RandVMF(:,1), RandVMF(:,2), RandVMF(:,3),3, 'b', 'filled'), axis([-1 1 -1 1 -1 1]);
