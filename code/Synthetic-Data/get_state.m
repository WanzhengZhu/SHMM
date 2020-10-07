function k = get_state(value, prob_distribution)

for j=1:length(prob_distribution)
    if sum(prob_distribution(1:j))>=value
        k = j;
        break;
    end
end

end