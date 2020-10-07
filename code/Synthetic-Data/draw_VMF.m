function draw_VMF(K, dim, kappa_value, num)
% K is number of Latent States
% dim is the dimension of vectors
K = 3; % Number of Latent States
dim = 3;
kappa_value = 10;
num = 400;
% Initialize parameters...
N = 2; % Length of the HMM
%Ini = [0.4 0.5 0.1 0];
%A = [0 1 0 0; 0 0 1 0; 0 0 0 1;1 0 0 0 ];
Ini = rand(1,K); Ini = Ini/sum(Ini); % Initial probability---Pi
A = rand(K,K); for i=1:K; A(i,:)=A(i,:)/sum(A(i,:)); end % Transition probability---A
mu = 2*rand(K,dim)-1; % Mean direction
mu = [-1 0 1; 0 -1 0; 0 0.3 -1];
for i=1:size(mu,1)
    mu(i,:) = mu(i,:)/norm(mu(i,:));
end
kappa = kappa_value * ones(K,1);
kappa = rand(K,1)*300; % Kappa
kappa = [10; 50; 50];
RandVMF = cell(K,1);
if length(Ini)~=K || size(A,1)~=K || size(A,2)~=K
    error('Pi and A Size error');
end
counter = ones(K,1);

% Read the data format...
text = textread('sequences_test.txt', '%s', 'delimiter', ',');
fileID = fopen('synthetic_data.txt','w');

% Generate VMF data...
D=num*6;
figure; 
[RandVMF{1}] = randVMF(D, mu(1,:), kappa(1,1));
[RandVMF{2}] = randVMF(D, mu(2,:), kappa(2,1));
[RandVMF{3}] = randVMF(D, mu(3,:), kappa(3,1));
scatter3(RandVMF{1}(:,1), RandVMF{1}(:,2), RandVMF{1}(:,3),3,'r','filled'), axis([-1 1 -1 1 -1 1]);
hold on;
scatter3(RandVMF{2}(:,1), RandVMF{2}(:,2), RandVMF{2}(:,3),3,'g','filled'), axis([-1 1 -1 1 -1 1]);
scatter3(RandVMF{3}(:,1), RandVMF{3}(:,2), RandVMF{3}(:,3),3,'b','filled'), axis([-1 1 -1 1 -1 1]);
[x,y,z] = sphere;
lightGrey = 0.8*[1 1 1]; % It looks better if the lines are lighter
surface(x,y,z,'FaceColor', 'none','EdgeColor',lightGrey)
plot3([0 mu(1,1)], [0 mu(1,2)], [0 mu(1,3)], 'r');
plot3([0 mu(2,1)], [0 mu(2,2)], [0 mu(2,3)], 'g');
plot3([0 mu(3,1)], [0 mu(3,2)], [0 mu(3,3)], 'b');
set(gca,'fontsize',25)
saveas(gcf,'VMF.png')

figure;
for k=1:K % States
    mu(k,:) = mu(k,:)/norm(mu(k,:)); % Normalize mean direction to unit length
    [RandVMF{k}] = randVMF(D, mu(k,:), kappa(k,1));
    %figure;
    scatter3(RandVMF{k}(:,1), RandVMF{k}(:,2), RandVMF{k}(:,3),3 , 'filled'), axis([-1 1 -1 1 -1 1]);
    hold on;
end
[x,y,z] = sphere;
lightGrey = 0.8*[1 1 1]; % It looks better if the lines are lighter
surface(x,y,z,'FaceColor', 'none','EdgeColor',lightGrey)

% Get initial state
dist = Ini;
state = get_state(rand(), dist);

% Write data vector to the file...
%for i=1:floor(size(text,1)/N/6)*N*6
for i=1:num*6
    if mod(i,6)==0 % Text vector
        % Get the state...
        state = get_state(rand(), dist);
        if mod(i,N*6)==0 % A new trajectory
            dist = Ini;
        else % A new state
            dist = A(state,:);
        end
        %disp(num2str(state));
        
        % Get the VMF generated data...
        for j=1:dim-1
            %temp = rand();
            %fprintf(fileID,'%d ',RandVMF{state}(1+round(temp*(D-1)),j));
            fprintf(fileID,'%d ',RandVMF{state}(counter(state),j));
        end
        %fprintf(fileID,'%d',RandVMF{state}(1+round(temp*(D-1)),end)); % Format issue
        fprintf(fileID,'%d',RandVMF{state}(counter(state),end)); % Format issue
        counter(state) = counter(state)+1;
        
        % Formatting...
        if mod(i,N*6)==0
            fprintf(fileID,'\n');
        else
            fprintf(fileID,',');
        end
        
    else
        fprintf(fileID,'%d',str2num(text{i}));
        fprintf(fileID,',');
    end
end
fclose(fileID);
%save('temp.mat');

% Write the basic information
fileID = fopen('data_para.txt','w');
fprintf(fileID,'%d\n',K);
for i=1:K
    for j=1:dim
        fprintf(fileID,'%f ',mu(i,j));
    end
    fprintf(fileID,'\n');
end
for i=1:K
    fprintf(fileID,'%f\n',kappa(i));
end
for i=1:K
    fprintf(fileID,'%f\n',Ini(i));
end
for i=1:K
    for j=1:K
        fprintf(fileID,'%f ',A(i,j));
    end
    fprintf(fileID,'\n');
end
fclose(fileID);
end